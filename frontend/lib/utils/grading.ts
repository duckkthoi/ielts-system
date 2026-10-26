// ─── Chấm bài phía client: chỉ để XEM TRƯỚC khi đang chấm (frontend/AI-CODING-RULES.md mục 8) ─
// Cùng quy tắc với server (GradingRules, BandCalculator — BR-11, BR-12, BR-17, BR-18); server luôn tính lại
// và là nơi quyết định. Hàm thuần, không import alias để `node --test` chạy trực tiếp.

/** BR-18: bài ≤ 20 từ thì mọi tiêu chí tối đa Band 1. */
export const SHORT_ESSAY_WORDS = 20;
export const SHORT_ESSAY_MAX_BAND = 1;

/** Làm tròn hiển thị kiểu IELTS: lẻ < .25 → xuống; .25 – < .75 → .5; ≥ .75 → lên. Ví dụ 6.583 → 6.5; 6.75 → 7. */
export function roundBand(raw: number | null): number | null {
  if (raw === null) return null;
  // Làm tròn 6 chữ số trước để tránh sai số dấu phẩy động (vd 6.749999…).
  const value = Math.round(raw * 1e6) / 1e6;
  const whole = Math.floor(value);
  const fraction = value - whole;
  if (fraction < 0.25) return whole;
  return fraction < 0.75 ? whole + 0.5 : whole + 1;
}

/** Band phần chưa làm tròn = trung bình có trọng số; null khi còn tiêu chí chưa chấm. */
export function partBandRaw(items: readonly { score: number | null; weight: number }[]): number | null {
  if (items.length === 0 || items.some((i) => i.score === null)) return null;
  const total = items.reduce((s, i) => s + i.weight, 0);
  if (total <= 0) return null;
  return items.reduce((s, i) => s + (i.score as number) * i.weight, 0) / total;
}

/**
 * Điểm tối đa được phép (BR-17, BR-18): bài ≤ 20 từ → 1; mục "Giới hạn điểm" đang tick → maxBand nhỏ nhất.
 * Trùng mức thì giữ lý do xuất hiện trước (giống server).
 */
export function scoreCap(
  maxScore: number,
  checkedLimits: readonly { content: string; maxBand: number | null }[],
  wordCount: number
): { max: number; reason: string | null } {
  let max = maxScore;
  let reason: string | null = null;
  if (wordCount <= SHORT_ESSAY_WORDS && SHORT_ESSAY_MAX_BAND < max) {
    max = SHORT_ESSAY_MAX_BAND;
    reason = `Bài chỉ có ${wordCount} từ (≤ ${SHORT_ESSAY_WORDS} từ)`;
  }
  for (const mark of checkedLimits) {
    if (mark.maxBand !== null && mark.maxBand < max) {
      max = mark.maxBand;
      reason = mark.content;
    }
  }
  return { max, reason };
}

/** Các mức điểm chọn được: 0 → maxScore theo bước (BR-11), vd bước 1 → 0, 1, …, 9. */
export function scoreOptions(maxScore: number, step: number): number[] {
  if (step <= 0) return [];
  const count = Math.round(maxScore / step);
  return Array.from({ length: count + 1 }, (_, i) => Math.round(i * step * 1e6) / 1e6);
}

/** Tick thêm mục giới hạn làm điểm đang chọn vượt mức → hạ xuống đúng mức tối đa được phép. */
export function clampScore(score: number | null, max: number): number | null {
  return score !== null && score > max ? max : score;
}
