// ─── Mô tả thời gian còn lại tới hạn (docs/07 mục 7.2 CountdownTimer) ─────────
// Hàm thuần, không import alias để `node --test` chạy trực tiếp. Chỉ dùng để HIỂN THỊ —
// quá hạn hay chưa do server quyết định (frontend/AI-CODING-RULES.md mục 8).

export type DueTone = 'normal' | 'warning' | 'error' | 'overdue';

const MINUTE = 60_000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

const pad = (n: number) => String(n).padStart(2, '0');

function hms(ms: number): string {
  const total = Math.floor(ms / 1000);
  return `${pad(Math.floor(total / 3600))}:${pad(Math.floor((total % 3600) / 60))}:${pad(total % 60)}`;
}

/**
 * > 24 giờ: "Còn 2 ngày"; ≤ 24 giờ: "Còn 05:12:30" (< 2 giờ: warning, < 15 phút: error);
 * đã qua: "Quá hạn 3 giờ" / "Quá hạn 2 ngày".
 */
export function describeDue(dueAt: string, now: number = Date.now()): { text: string; tone: DueTone } {
  const left = Date.parse(dueAt) - now;
  if (left <= 0) {
    const late = -left;
    if (late < HOUR) return { text: `Quá hạn ${Math.max(1, Math.floor(late / MINUTE))} phút`, tone: 'overdue' };
    if (late < DAY) return { text: `Quá hạn ${Math.floor(late / HOUR)} giờ`, tone: 'overdue' };
    return { text: `Quá hạn ${Math.floor(late / DAY)} ngày`, tone: 'overdue' };
  }
  if (left > DAY) return { text: `Còn ${Math.floor(left / DAY)} ngày`, tone: 'normal' };
  const tone: DueTone = left < 15 * MINUTE ? 'error' : left < 2 * HOUR ? 'warning' : 'normal';
  return { text: `Còn ${hms(left)}`, tone };
}
