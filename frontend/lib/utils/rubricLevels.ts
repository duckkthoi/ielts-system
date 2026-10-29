// ─── Mức đạt của tiêu chí bài PAPER (rubric phân tích, BR-31) ─────────────────
// Kiểm tra trước ở client cùng quy tắc với server (RubricRules.validateLevels) để báo ngay trên form; server kiểm lại.

export interface LevelInput {
  label?: string | null;
  minScore?: number | null;
  maxScore?: number | null;
}

export const MAX_LEVELS = 6;

const isMultiple = (value: number, step: number) => Math.abs(value / step - Math.round(value / step)) < 1e-9;
const plain = (n: number) => String(Number(n.toFixed(2)));

/** Lỗi của danh sách mức (rỗng = hợp lệ). Không có mức nào là hợp lệ (chấm điểm trực tiếp). */
export function levelProblems(levels: LevelInput[], maxScore: number, step: number): string[] {
  if (levels.length === 0) return [];
  const problems: string[] = [];
  if (levels.length < 2) problems.push('Cần ít nhất 2 mức đạt');
  if (levels.length > MAX_LEVELS) problems.push(`Tối đa ${MAX_LEVELS} mức đạt`);
  const seen = new Set<string>();
  for (const l of levels) {
    const name = (l.label ?? '').trim();
    if (!name) {
      problems.push('Mức đạt cần có tên');
      continue;
    }
    if (seen.has(name.toLowerCase())) problems.push(`Mức "${name}" bị lặp`);
    seen.add(name.toLowerCase());
    if (l.minScore == null || l.maxScore == null) {
      problems.push(`Mức "${name}" cần đủ điểm thấp nhất và cao nhất`);
      continue;
    }
    if (l.minScore < 0 || l.minScore > l.maxScore || l.maxScore > maxScore) {
      problems.push(`Mức "${name}": cần 0 ≤ thấp nhất ≤ cao nhất ≤ ${plain(maxScore)}`);
    } else if (!isMultiple(l.minScore, step) || !isMultiple(l.maxScore, step)) {
      problems.push(`Mức "${name}": điểm phải là bội số của ${plain(step)}`);
    }
  }
  const ranged = levels
    .filter((l) => l.minScore != null && l.maxScore != null)
    .sort((a, b) => (a.minScore as number) - (b.minScore as number));
  for (let i = 1; i < ranged.length; i++) {
    if ((ranged[i].minScore as number) <= (ranged[i - 1].maxScore as number)) {
      problems.push(
        `Mức "${(ranged[i - 1].label ?? '').trim()}" và "${(ranged[i].label ?? '').trim()}" chồng khoảng điểm`
      );
    }
  }
  return problems;
}

/**
 * 4 mức mặc định chia đều thang điểm (Xuất sắc / Tốt / Đạt / Chưa đạt), mức trên bắt đầu ở 90 %, 70 %, 50 % thang —
 * thang 10 bước 0,25: 9–10, 7–8,75, 5–6,75, 0–4,75 (docs/12 mục 5.4).
 */
export function defaultLevels(maxScore: number, step: number): { label: string; minScore: number; maxScore: number }[] {
  const at = (ratio: number) => Math.round((maxScore * ratio) / step) * step;
  const excellent = at(0.9);
  const good = at(0.7);
  const pass = at(0.5);
  return [
    { label: 'Xuất sắc', minScore: excellent, maxScore },
    { label: 'Tốt', minScore: good, maxScore: excellent - step },
    { label: 'Đạt', minScore: pass, maxScore: good - step },
    { label: 'Chưa đạt', minScore: 0, maxScore: pass - step }
  ];
}
