// ─── Biểu đồ "Band theo thời gian" (UC-28) ────────────────────────────────────
// Hàm thuần (có test): tách chuỗi band cả bài / Task 1 / Task 2 theo thời điểm trả bài,
// tính miền trục band và trục thời gian. Phần "Tự do" (CUSTOM) không vẽ — vẫn có trong bảng.

import type { AssignmentBand, PartBand } from '@/lib/types/stats';

export type TrendSeriesKey = 'overall' | 'task1' | 'task2';

export interface TrendPoint {
  key: string;
  /** Tên bài (và phần) để hiện trong tooltip. */
  label: string;
  band: number;
  /** Thời điểm trả bài (ms). */
  at: number;
}

export interface TrendSeries {
  key: TrendSeriesKey;
  label: string;
  points: TrendPoint[];
}

const byTime = (a: TrendPoint, b: TrendPoint) => a.at - b.at;

/** Thứ tự chuỗi cố định (màu theo chuỗi, không theo số chuỗi còn lại); chuỗi rỗng bị bỏ. */
export function buildBandTrend(assignments: AssignmentBand[], parts: PartBand[]): TrendSeries[] {
  const partPoints = (type: PartBand['taskType']) =>
    parts
      .filter((p) => p.taskType === type && p.band !== null)
      .map((p) => ({
        key: `part-${p.partId}`,
        label: `${p.assignmentTitle} · ${p.partTitle}`,
        band: p.band as number,
        at: Date.parse(p.returnedAt)
      }))
      .sort(byTime);

  const series: TrendSeries[] = [
    {
      key: 'overall',
      label: 'Band cả bài',
      points: assignments
        .filter((a) => a.complete && a.band !== null)
        .map((a) => ({ key: `asg-${a.assignmentId}`, label: a.title, band: a.band as number, at: Date.parse(a.returnedAt) }))
        .sort(byTime)
    },
    { key: 'task1', label: 'Task 1', points: partPoints('WRITING_TASK1') },
    { key: 'task2', label: 'Task 2', points: partPoints('WRITING_TASK2') }
  ];
  return series.filter((s) => s.points.length > 0);
}

/** Miền trục band: nới 1 band mỗi phía, trong [0, 9], rộng ít nhất 2 band. Không có dữ liệu → [0, 9]. */
export function bandDomain(bands: number[]): [number, number] {
  if (bands.length === 0) return [0, 9];
  let lo = Math.max(0, Math.floor(Math.min(...bands)) - 1);
  let hi = Math.min(9, Math.ceil(Math.max(...bands)) + 1);
  if (hi - lo < 2) {
    hi = Math.min(9, lo + 2);
    lo = Math.max(0, hi - 2);
  }
  return [lo, hi];
}

/** Đổi thời điểm sang vị trí [0, 1] trên trục ngang; chỉ một thời điểm → giữa trục. */
export function timeScale(times: number[]): (t: number) => number {
  if (times.length === 0) return () => 0.5;
  const min = Math.min(...times);
  const max = Math.max(...times);
  if (max === min) return () => 0.5;
  return (t) => (t - min) / (max - min);
}
