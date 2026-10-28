// ─── Màn giám sát chế độ thi (UC-21, BR-25) ───────────────────────────────────
// Áp sự kiện `/topic/assignments/{id}/exam` vào danh sách phiên lấy từ REST, để bảng đổi ngay không cần tải lại.
// Hàm thuần (có test).

import type { ExamPush, MonitorRow } from '@/lib/types/exam';

/** Số sự kiện tối đa giữ trong bảng tin bên phải. */
export const FEED_LIMIT = 50;

export const examCellKey = (studentId: number, partId: number) => `${studentId}:${partId}`;

/**
 * Cập nhật phiên tương ứng với sự kiện. Phiên chưa có trong danh sách (vừa bắt đầu, hoặc sinh viên ngoài bộ lọc
 * lớp) → `unknown = true`, trang tự tải lại từ REST (server mới biết sinh viên có thuộc lớp đang lọc không).
 */
export function applyExamPush(rows: MonitorRow[], push: ExamPush): { rows: MonitorRow[]; unknown: boolean } {
  const index = rows.findIndex((r) => r.sessionId === push.sessionId);
  if (index < 0) return { rows, unknown: true };
  const r = rows[index];
  const next: MonitorRow = {
    ...r,
    status: push.status,
    lastEventAt: push.at,
    // "Rời trang" gồm ẩn tab và rời cửa sổ (server đếm giống vậy).
    tabHiddenCount: r.tabHiddenCount + (push.type === 'TAB_HIDDEN' || push.type === 'WINDOW_BLUR' ? 1 : 0),
    pasteCount: r.pasteCount + (push.type === 'PASTE' ? 1 : 0),
    disconnectCount: r.disconnectCount + (push.type === 'DISCONNECT' ? 1 : 0),
    online: push.type === 'DISCONNECT' ? false : push.type === 'RECONNECT' ? true : r.online,
    finishedAt: push.type === 'SUBMITTED' ? push.at : r.finishedAt
  };
  return { rows: rows.map((x, i) => (i === index ? next : x)), unknown: false };
}

/** Thêm sự kiện mới lên đầu bảng tin, giữ tối đa `FEED_LIMIT`. Server có thể phát lại cùng sự kiện khi kết nối lại. */
export function prependFeed(feed: ExamPush[], push: ExamPush, limit: number = FEED_LIMIT): ExamPush[] {
  const same = (x: ExamPush) => x.sessionId === push.sessionId && x.type === push.type && x.at === push.at;
  if (feed.some(same)) return feed;
  return [push, ...feed].slice(0, limit);
}

/**
 * Bảng tin lúc mở trang = sự kiện gần nhất từ REST, gộp với tin đẩy nhận được trong lúc chờ REST; bỏ trùng,
 * mới xảy ra trước, tối đa `limit`.
 */
export function mergeFeed(rest: ExamPush[], live: ExamPush[], limit: number = FEED_LIMIT): ExamPush[] {
  const key = (x: ExamPush) => `${x.sessionId}|${x.type}|${x.at}`;
  const seen = new Set<string>();
  return [...live, ...rest]
    .filter((x) => !seen.has(key(x)) && seen.add(key(x)))
    .sort((a, b) => Date.parse(b.at) - Date.parse(a.at))
    .slice(0, limit);
}

/** Số liệu thẻ tóm tắt. */
export function monitorSummary(rows: MonitorRow[]) {
  return {
    inProgress: rows.filter((r) => r.status === 'IN_PROGRESS').length,
    overtime: rows.filter((r) => r.status === 'OVERTIME').length,
    finished: rows.filter((r) => r.status === 'FINISHED' || r.status === 'FINISHED_OVERTIME').length,
    flagged: rows.filter((r) => r.tabHiddenCount > 0 || r.pasteCount > 0).length
  };
}
