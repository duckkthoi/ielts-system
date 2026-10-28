// ─── Chế độ thi phía client (docs/06 mục 8, BR-23 → BR-25) ────────────────────
// Chỉ để hiển thị: server mới là nơi quyết định quá giờ. Hàm thuần (có test).

import type { ExamEventType, ExamStatus } from '@/lib/types/exam';

/** Trạng thái phiên tại thời điểm `now` (ms, giờ server) — cùng quy tắc với `ExamRules` ở backend. */
export function examStatusAt(endsAt: string, finishedAt: string | null, now: number): ExamStatus {
  const ends = Date.parse(endsAt);
  if (finishedAt) return Date.parse(finishedAt) > ends ? 'FINISHED_OVERTIME' : 'FINISHED';
  return now < ends ? 'IN_PROGRESS' : 'OVERTIME';
}

/**
 * Đồng hồ thi: còn bao lâu ("12:05", "1:02:03") hoặc đã quá bao lâu ("+03:10").
 * Làm tròn lên khi còn giờ để không hiện "00:00" trong khi thực ra còn gần một giây.
 */
export function formatExamClock(endsAt: string, now: number): { text: string; overtime: boolean; urgent: boolean } {
  const diff = Date.parse(endsAt) - now;
  const overtime = diff <= 0;
  const total = overtime ? Math.floor(-diff / 1000) : Math.ceil(diff / 1000);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  const pad = (n: number) => String(n).padStart(2, '0');
  const body = h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`;
  return { text: overtime ? `+${body}` : body, overtime, urgent: !overtime && diff <= 5 * 60_000 };
}

/** Đang rời bài kiểu nào: ẩn tab (chuyển tab, thu nhỏ) hay tab vẫn hiện mà cửa sổ mất focus (Alt+Tab, màn hình 2). */
export type AwayKind = 'TAB' | 'WINDOW' | null;

/**
 * Bước tiếp theo khi trạng thái trang đổi (BR-25): `hidden` = tab bị ẩn, `focused` = `document.hasFocus()`.
 * Một lần chuyển tab trình duyệt bắn cả `blur` lẫn `visibilitychange` — gộp lại thành đúng một sự kiện rời và một
 * sự kiện quay lại, để không đếm hai lần.
 */
export function nextAway(
  prev: AwayKind,
  hidden: boolean,
  focused: boolean
): { away: AwayKind; event: ExamEventType | null } {
  if (hidden) return { away: 'TAB', event: prev === 'TAB' ? null : 'TAB_HIDDEN' };
  if (!focused) {
    if (prev === 'WINDOW') return { away: 'WINDOW', event: null };
    // Tab hiện lại nhưng focus vẫn ở cửa sổ khác: đã quay lại tab, vẫn đang rời cửa sổ.
    return { away: 'WINDOW', event: prev === 'TAB' ? 'TAB_VISIBLE' : 'WINDOW_BLUR' };
  }
  if (prev === null) return { away: null, event: null };
  return { away: null, event: prev === 'TAB' ? 'TAB_VISIBLE' : 'WINDOW_FOCUS' };
}
