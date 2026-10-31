// ─── Ai đang mở tài liệu Paper của nhóm (kênh awareness, docs/05 B) ───────────
// Server chỉ chuyển tiếp `{userId, userName, state}`, không lưu. Mỗi thành viên gửi nhịp 15 giây kèm mục đang soạn;
// rời màn thì gửi `active: false`. Ai im quá 40 giây coi như đã rời (mất mạng, đóng tab không kịp báo).

export const PRESENCE_HEARTBEAT_MS = 15_000;
export const PRESENCE_TIMEOUT_MS = 40_000;

export interface PresenceState {
  v: 1;
  active: boolean;
  /** `sectionKey` của mục đang đặt con trỏ. */
  section: string | null;
}

export interface PresencePeer {
  userId: number;
  userName: string;
  section: string | null;
  lastSeen: number;
}

export function encodePresence(active: boolean, section: string | null): string {
  const state: PresenceState = { v: 1, active, section };
  return JSON.stringify(state);
}

/** State lạ (client cũ, con trỏ Yjs…) → null, bỏ qua. */
export function decodePresence(raw: string): PresenceState | null {
  try {
    const value = JSON.parse(raw) as Partial<PresenceState>;
    if (value?.v !== 1 || typeof value.active !== 'boolean') return null;
    return { v: 1, active: value.active, section: typeof value.section === 'string' ? value.section : null };
  } catch {
    return null;
  }
}

/** Gộp một tin awareness vào bảng người đang mở (tin `active: false` → xoá). */
export function applyPresence(
  peers: Record<number, PresencePeer>,
  push: { userId: number; userName: string; state: string },
  now: number
): Record<number, PresencePeer> {
  const state = decodePresence(push.state);
  if (!state) return peers;
  const next = { ...peers };
  if (state.active) {
    next[push.userId] = { userId: push.userId, userName: push.userName, section: state.section, lastSeen: now };
  } else {
    delete next[push.userId];
  }
  return next;
}

/** Người còn trong hạn 40 giây, theo tên. */
export function onlinePeers(peers: Record<number, PresencePeer>, now: number): PresencePeer[] {
  return Object.values(peers)
    .filter((p) => now - p.lastSeen <= PRESENCE_TIMEOUT_MS)
    .sort((a, b) => a.userName.localeCompare(b.userName, 'vi'));
}
