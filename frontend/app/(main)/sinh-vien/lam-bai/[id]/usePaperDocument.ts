'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

import { prosemirrorJSONToYXmlFragment, yXmlFragmentToProsemirrorJSON } from '@tiptap/y-tiptap';

import { paperSchema } from '@/components/PaperEditor';
import { paperDocApi } from '@/lib/api/paperDoc';
import { ApiError } from '@/lib/apiError';
import { PaperSync, type SyncStatus } from '@/lib/paperSync';
import type { SectionDto } from '@/lib/types/assignment';
import { docText, initialDoc, type DocNode } from '@/lib/utils/paperDoc';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

// ─── Tài liệu paper của nhóm cho một giai đoạn (docs/12 mục 5.2) ──────────────
// Mở tài liệu (server tạo nếu chưa có) → `PaperSync` áp trạng thái và bắt đầu nghe / gửi update.
// Tài liệu mới tinh thì dựng khung mục từ cấu hình của giáo viên. Kết nối WebSocket lại → gửi nốt + lấy bù.
// Snapshot (kèm JSON + chữ thuần cho server) mỗi 30 giây, khi rời tab và khi rời màn.

const SNAPSHOT_MS = 30_000;

export type PaperLoadError = 'NO_GROUP' | 'FAILED';

/** JSON + chữ thuần hiện tại, đọc thẳng từ Yjs (không cần trình soạn). */
function currentContent(sync: PaperSync) {
  const json = yXmlFragmentToProsemirrorJSON(sync.doc.getXmlFragment('default')) as DocNode;
  return { json, text: docText(json) };
}

export function usePaperDocument(
  partId: number,
  sections: SectionDto[],
  onSnapshot?: (contentText: string) => void | Promise<void>
) {
  const [sync, setSync] = useState<PaperSync | null>(null);
  const [status, setStatus] = useState<SyncStatus>('loading');
  const [editable, setEditable] = useState(false);
  const [groupId, setGroupId] = useState<number | null>(null);
  const [error, setError] = useState<PaperLoadError | null>(null);
  const [attempt, setAttempt] = useState(0);
  // Trang tải lại chi tiết bài thì `sections` là mảng mới — không được mở lại tài liệu vì thế.
  const sectionsRef = useRef(sections);
  const connected = useRealtimeStore((s) => s.status) === 'CONNECTED';
  const wasConnected = useRef(connected);
  const onSnapshotRef = useRef(onSnapshot);

  useEffect(() => {
    onSnapshotRef.current = onSnapshot;
  }, [onSnapshot]);

  useEffect(() => {
    let cancelled = false;
    let opened: PaperSync | null = null;
    paperDocApi
      .open(partId)
      .then((state) => {
        if (cancelled) return;
        const s: PaperSync = new PaperSync(state.documentId, setStatus, () => currentContent(s).text.length);
        opened = s;
        s.start(state);
        if (state.editable) {
          s.initializeIfEmpty((fragment) =>
            prosemirrorJSONToYXmlFragment(paperSchema(), initialDoc(sectionsRef.current), fragment)
          );
        }
        setEditable(state.editable);
        setGroupId(state.groupId);
        setSync(s);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiError && err.code === 'NO_GROUP' ? 'NO_GROUP' : 'FAILED');
      });
    return () => {
      cancelled = true;
      if (opened) {
        // Trạng thái được mã hoá ngay khi gọi, nên hủy tài liệu sau đó vẫn an toàn.
        const { json, text } = currentContent(opened);
        void opened.snapshot(json, text);
        opened.destroy();
      }
      setSync(null);
    };
  }, [partId, attempt]);

  useEffect(() => {
    if (connected && !wasConnected.current) void sync?.onReconnected();
    wasConnected.current = connected;
  }, [connected, sync]);

  /** `force`: lưu cả khi máy này không sửa gì (trước khi nộp). */
  const snapshotNow = useCallback(
    async (force = false): Promise<string | null> => {
      if (!sync) return null;
      const { json, text } = currentContent(sync);
      await sync.snapshot(json, text, force);
      if (text.trim()) await onSnapshotRef.current?.(text);
      return text;
    },
    [sync]
  );

  useEffect(() => {
    if (!sync || !editable) return;
    const timer = setInterval(() => void snapshotNow(), SNAPSHOT_MS);
    const onHide = () => {
      if (document.visibilityState === 'hidden') void snapshotNow();
    };
    document.addEventListener('visibilitychange', onHide);
    return () => {
      clearInterval(timer);
      document.removeEventListener('visibilitychange', onHide);
    };
  }, [sync, editable, snapshotNow]);

  const retry = useCallback(() => {
    setError(null);
    setStatus('loading');
    setAttempt((a) => a + 1);
  }, []);

  return { sync, status, editable, groupId, error, retry, snapshotNow };
}
