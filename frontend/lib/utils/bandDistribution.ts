// ─── Biểu đồ phân bố band (UC-19) ─────────────────────────────────────────────
// Hàm thuần (có test): server chỉ trả các band có bài; biểu đồ cần đủ mọi mức 0.5 ở giữa
// (cột trống = 0 bài) để khoảng cách trên trục đúng.

import type { BandCount } from '@/lib/types/stats';

const STEP = 0.5;

/** Đủ các mức từ band nhỏ nhất tới lớn nhất có bài, bước 0.5; không có dữ liệu → []. */
export function fillBandBins(distribution: BandCount[]): BandCount[] {
  if (distribution.length === 0) return [];
  const counts = new Map(distribution.map((d) => [d.band * 2, d.count]));
  const min = Math.min(...distribution.map((d) => d.band)) * 2;
  const max = Math.max(...distribution.map((d) => d.band)) * 2;
  const bins: BandCount[] = [];
  for (let half = min; half <= max; half++) bins.push({ band: half * STEP, count: counts.get(half) ?? 0 });
  return bins;
}

/** Tỉ lệ nộp đúng hạn gộp mọi phần: (đã nộp − muộn) / đã nộp; chưa ai nộp → null. */
export function overallOnTimeRate(parts: { submittedCount: number; lateCount: number }[]): number | null {
  const submitted = parts.reduce((sum, p) => sum + p.submittedCount, 0);
  if (submitted === 0) return null;
  const late = parts.reduce((sum, p) => sum + p.lateCount, 0);
  return (submitted - late) / submitted;
}
