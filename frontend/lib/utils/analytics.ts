import type { Ratio } from '@/lib/types/analytics';

// ─── Hiển thị thống kê tổng hợp (H5) — hàm thuần, có test ─────────────────────

/** "62% (31/50)"; mẫu số 0 → "—". */
export function formatRatio(r: Ratio): string {
  if (r.rate === null || r.denominator === 0) return '—';
  return `${Math.round(r.rate * 100)}% (${r.numerator}/${r.denominator})`;
}

/** Chênh lệch so với kỳ trước: `null` khi thiếu một bên. `percent` = điểm phần trăm (cho tỉ lệ). */
export function delta(current: number | null, previous: number | null): number | null {
  if (current === null || previous === null) return null;
  return Math.round((current - previous) * 100) / 100;
}

/** Bản đồ nhiệt đủ 7 × 24 ô (thiếu = 0), hàng = thứ Hai → Chủ nhật. */
export function heatGrid(cells: { dow: number; hour: number; count: number }[]): number[][] {
  const grid = Array.from({ length: 7 }, () => Array<number>(24).fill(0));
  for (const c of cells) {
    if (c.dow >= 1 && c.dow <= 7 && c.hour >= 0 && c.hour < 24) grid[c.dow - 1][c.hour] = c.count;
  }
  return grid;
}

/** Mức đậm 0 – 4 của một ô theo giá trị lớn nhất (0 = không có hoạt động). */
export function heatLevel(value: number, max: number): number {
  if (value <= 0 || max <= 0) return 0;
  return Math.min(4, Math.ceil((value / max) * 4));
}
