// ─── Dashboard giáo viên: tổng hợp từ danh sách bài đang mở ───────────────────
// Hàm thuần (có test). Chỉ để hiển thị; "now" là giờ server (serverNow) do trang truyền vào.

import type { AssignmentSummary } from '@/lib/types/assignment';

/** Bài coi là "sắp hết hạn" khi còn ít hơn chừng này. */
export const DUE_SOON_MS = 48 * 60 * 60 * 1000;

export interface OpenAssignmentSummary {
  open: number;
  dueSoon: number;
  /** Người nhận đã nộp đủ mọi phần / tổng người nhận, cộng dồn các bài đang mở. */
  completed: number;
  recipients: number;
  pendingGrading: number;
}

export function summarizeOpenAssignments(items: AssignmentSummary[], now: number): OpenAssignmentSummary {
  const open = items.filter((a) => a.status === 'PUBLISHED');
  return {
    open: open.length,
    dueSoon: open.filter((a) => {
      if (!a.dueAt) return false;
      const left = Date.parse(a.dueAt) - now;
      return left > 0 && left <= DUE_SOON_MS;
    }).length,
    completed: open.reduce((sum, a) => sum + a.completedCount, 0),
    recipients: open.reduce((sum, a) => sum + a.recipientCount, 0),
    pendingGrading: open.reduce((sum, a) => sum + a.pendingGradingCount, 0)
  };
}

/** Bài đang mở, hạn gần nhất trước; bài không có hạn xếp cuối. */
export function sortByDue(items: AssignmentSummary[]): AssignmentSummary[] {
  return [...items].sort((a, b) => {
    if (!a.dueAt) return b.dueAt ? 1 : 0;
    if (!b.dueAt) return -1;
    return Date.parse(a.dueAt) - Date.parse(b.dueAt);
  });
}
