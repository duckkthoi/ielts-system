import type { LiveComment, LiveCommentPush } from '@/lib/types/paper';

// ─── Nhận xét trực tiếp trên Paper đang viết (H4) — hàm thuần, có test ────────
// Offset của nhận xét tính trên chữ thuần lúc tạo. Nhóm sửa tiếp thì chữ dịch đi: chỉ tô khi đoạn tại offset vẫn đúng
// bằng `quote`; lệch thì coi là "Đoạn văn đã thay đổi" — không dò tìm chỗ khác để tránh gắn nhầm sang đoạn khác.

export function isAnchored(text: string, c: Pick<LiveComment, 'startOffset' | 'endOffset' | 'quote'>): boolean {
  return c.endOffset <= text.length && text.slice(c.startOffset, c.endOffset) === c.quote;
}

/** Gộp một sự kiện realtime vào danh sách (giữ thứ tự theo vị trí rồi id, như server trả). */
export function applyCommentPush(list: LiveComment[], push: LiveCommentPush): LiveComment[] {
  const rest = list.filter((c) => c.id !== push.commentId);
  if (push.type === 'DELETED' || !push.comment) return rest;
  return [...rest, push.comment].sort((a, b) => a.startOffset - b.startOffset || a.id - b.id);
}

/** Số nhận xét chưa xử lý. */
export const openCount = (list: LiveComment[]) => list.filter((c) => !c.resolvedAt).length;
