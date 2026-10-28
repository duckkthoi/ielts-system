import * as Y from 'yjs';

import { paperDocApi } from '@/lib/api/paperDoc';
import { publishRealtime, subscribeRealtime } from '@/lib/realtime';
import type { DocUpdate, DocumentState, UpdatePush } from '@/lib/types/paper';
import { fromBase64, syncDecision, toBase64 } from '@/lib/utils/paperDoc';

// ─── Đồng bộ tài liệu paper Yjs với server (docs/12 mục 5.2, docs/05 B.2) ─────
// Server chỉ cấp số thứ tự, lưu và phát lại update nhị phân. Ở đây:
// - Nạp: áp snapshot + các update sau nó, nhớ `lastSeq`.
// - Gõ: gom update của máy này ~300 ms rồi gửi `/app/paper/{id}/update`; gửi không được (mất mạng) thì giữ lại.
// - Nhận `/topic/paper/{id}`: seq kế tiếp → áp; đã có → bỏ; hở số → lấy bù qua REST (gộp mất rồi thì tải lại cả).
// - Update của chính mình cũng được phát lại (áp lại Yjs không đổi gì) — nhờ đó `lastSeq` luôn khớp server.
// - Snapshot định kỳ do màn hình gọi (`snapshot`), kèm JSON + chữ thuần để server đếm từ / nộp / xuất file.

/** Origin của update đến từ server — không gửi ngược lại. */
const REMOTE = Symbol('paper-remote');
const FLUSH_MS = 300;
/** Server nhận tối đa 32 KB / update (base64 phải lọt khung STOMP 64 KB). */
const MAX_UPDATE_BYTES = 30 * 1024;
/** Client cố định để mọi thành viên tạo cùng một bản khởi tạo khung mục → Yjs coi là một (không nhân đôi mục). */
const INIT_CLIENT_ID = 1;

export type SyncStatus = 'loading' | 'synced' | 'sending' | 'offline' | 'error';

const newClientId = () => globalThis.crypto?.randomUUID?.() ?? `${Date.now().toString(36)}-${Math.random()}`;

export class PaperSync {
  readonly doc = new Y.Doc();
  private lastSeq = 0;
  private snapshotSeq = 0;
  private pending: Uint8Array[] = [];
  private flushTimer: ReturnType<typeof setTimeout> | null = null;
  private unsubscribe: (() => void) | null = null;
  private catchingUp = false;
  private changedSinceSnapshot = false;
  private destroyed = false;

  constructor(
    readonly documentId: number,
    private readonly onStatus: (status: SyncStatus) => void,
    /** Độ dài chữ hiện tại — để ước số ký tự thêm / xoá cho thống kê đóng góp (BR-32). */
    private readonly textLength: () => number
  ) {}

  private lastLength = 0;

  /** Áp trạng thái ban đầu, rồi bắt đầu nghe / gửi. */
  start(state: DocumentState) {
    this.applyState(state);
    this.lastLength = this.textLength();
    this.doc.on('update', this.onLocalUpdate);
    this.unsubscribe = subscribeRealtime(`/topic/paper/${this.documentId}`, (payload) =>
      this.onPush(payload as UpdatePush)
    );
    this.onStatus('synced');
  }

  /** Tài liệu mới tinh (chưa ai viết) → dựng khung mục; mọi thành viên dựng giống hệt nên không bị nhân đôi. */
  initializeIfEmpty(build: (fragment: Y.XmlFragment) => void) {
    if (this.doc.getXmlFragment('default').length > 0) return;
    const seed = new Y.Doc();
    seed.clientID = INIT_CLIENT_ID;
    build(seed.getXmlFragment('default'));
    Y.applyUpdate(this.doc, Y.encodeStateAsUpdate(seed));
    seed.destroy();
  }

  /** Kết nối lại: gửi nốt phần đang giữ, lấy bù phần bị lỡ. */
  async onReconnected() {
    this.flush();
    await this.catchUp();
  }

  /**
   * Gửi snapshot nếu có thay đổi kể từ lần trước (lỗi thì lần sau thử lại). `force`: gửi cả khi máy này không sửa gì —
   * trước khi nộp, vì thay đổi của thành viên khác cũng phải nằm trong snapshot (server nộp từ snapshot — B25a).
   */
  async snapshot(contentJson: unknown, contentText: string, force = false) {
    if ((!this.changedSinceSnapshot && !force) || this.lastSeq === 0) return;
    this.changedSinceSnapshot = false;
    try {
      const state = await paperDocApi.saveSnapshot(this.documentId, {
        state: toBase64(Y.encodeStateAsUpdate(this.doc)),
        upToSeq: this.lastSeq,
        contentJson: JSON.stringify(contentJson),
        contentText
      });
      this.snapshotSeq = state.snapshotSeq;
    } catch {
      this.changedSinceSnapshot = true;
    }
  }

  destroy() {
    this.destroyed = true;
    if (this.flushTimer) clearTimeout(this.flushTimer);
    this.flush();
    this.unsubscribe?.();
    this.doc.off('update', this.onLocalUpdate);
    this.doc.destroy();
  }

  // ─── Nội bộ ───

  private applyState(state: DocumentState) {
    Y.transact(
      this.doc,
      () => {
        if (state.snapshot) Y.applyUpdate(this.doc, fromBase64(state.snapshot), REMOTE);
        state.updates.forEach((u) => Y.applyUpdate(this.doc, fromBase64(u.update), REMOTE));
      },
      REMOTE
    );
    this.lastSeq = state.lastSeq;
    this.snapshotSeq = state.snapshotSeq;
  }

  private onLocalUpdate = (update: Uint8Array, origin: unknown) => {
    if (origin === REMOTE || this.destroyed) return;
    this.pending.push(update);
    this.changedSinceSnapshot = true;
    if (!this.flushTimer) this.flushTimer = setTimeout(() => this.flush(), FLUSH_MS);
  };

  private flush() {
    this.flushTimer = null;
    if (this.pending.length === 0) return;
    const merged = Y.mergeUpdates(this.pending);
    if (merged.length > MAX_UPDATE_BYTES) {
      // Hiếm (dán một khối rất lớn): gửi từng update; cái nào vẫn quá lớn thì server báo lỗi riêng cho người gửi.
      const all = this.pending;
      this.pending = [];
      all.forEach((u) => this.send(u));
      return;
    }
    this.pending = [];
    this.send(merged);
  }

  private send(update: Uint8Array) {
    const length = this.textLength();
    const delta = length - this.lastLength;
    const ok = publishRealtime(`/app/paper/${this.documentId}/update`, {
      update: toBase64(update),
      clientId: newClientId(),
      charsAdded: Math.max(0, delta),
      charsRemoved: Math.max(0, -delta)
    });
    if (ok) {
      this.lastLength = length;
      this.onStatus('sending');
    } else {
      // Mất kết nối: giữ lại, gửi khi có mạng (onReconnected). Yjs gộp được nên không mất chữ.
      this.pending.unshift(update);
      this.onStatus('offline');
    }
  }

  private onPush(p: UpdatePush) {
    if (this.destroyed) return;
    const decision = syncDecision(this.lastSeq, p.seq);
    if (decision === 'skip') return;
    if (decision === 'gap') {
      void this.catchUp();
      return;
    }
    Y.applyUpdate(this.doc, fromBase64(p.update), REMOTE);
    this.lastSeq = p.seq;
    if (this.pending.length === 0) this.onStatus('synced');
  }

  private async catchUp() {
    if (this.catchingUp || this.destroyed) return;
    this.catchingUp = true;
    try {
      const res = await paperDocApi.updatesAfter(this.documentId, this.lastSeq);
      if (res.resync) {
        this.applyState(await paperDocApi.state(this.documentId));
      } else {
        this.applyUpdates(res.updates);
        this.lastSeq = Math.max(this.lastSeq, res.lastSeq);
      }
      this.onStatus(this.pending.length ? 'offline' : 'synced');
    } catch {
      this.onStatus('error');
    } finally {
      this.catchingUp = false;
    }
  }

  private applyUpdates(updates: DocUpdate[]) {
    Y.transact(this.doc, () => updates.forEach((u) => Y.applyUpdate(this.doc, fromBase64(u.update), REMOTE)), REMOTE);
  }
}
