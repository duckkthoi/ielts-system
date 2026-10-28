// ─── Đánh dấu trên bài kiểu "Word đã chấm" (F5b, docs/08 mục 4.3) ─────────────
// Hàm thuần, không import alias để `node --test` chạy trực tiếp. Chữ gốc của sinh viên KHÔNG đổi:
// sửa (thay / xoá / thêm chữ) chỉ là lớp hiển thị. Offset theo đơn vị UTF-16 (String.length của JS) như server.

export type AnnKind = 'ERROR' | 'HIGHLIGHT' | 'GOOD' | 'COMMENT';
export type AnnEdit = 'REPLACE' | 'DELETE' | 'INSERT';

export interface AnnLike {
  key: string;
  startOffset: number;
  endOffset: number;
  kind: AnnKind;
  errorCode: string | null;
  comment: string | null;
  suggestion: string | null;
  edit: AnnEdit | null;
}

/** Lớp bật / tắt được: nền vàng-xanh · sửa (track changes) · lỗi (gạch gợn sóng) · nhận xét (nền xám + lề). */
export interface Layers {
  highlight: boolean;
  edit: boolean;
  error: boolean;
  comment: boolean;
}

export const ALL_LAYERS: Layers = { highlight: true, edit: true, error: true, comment: true };

/** Kiểu sửa thực tế: bản ghi cũ có gợi ý mà không có kiểu sửa = thay thế. */
export function effectiveEdit(a: Pick<AnnLike, 'kind' | 'edit' | 'suggestion'>): AnnEdit | null {
  if (a.edit) return a.edit;
  return a.kind === 'ERROR' && a.suggestion ? 'REPLACE' : null;
}

/** Đánh dấu có hiện ra không khi một số lớp đang tắt. */
export function isVisible(a: AnnLike, layers: Layers): boolean {
  if (a.kind === 'HIGHLIGHT' || a.kind === 'GOOD') return layers.highlight;
  if (a.kind === 'COMMENT') return layers.comment;
  return (
    (layers.error && a.errorCode !== null) ||
    (layers.edit && effectiveEdit(a) !== null) ||
    (layers.comment && a.comment !== null)
  );
}

export interface Segment {
  start: number;
  end: number;
  text: string;
  /** Các đánh dấu (đang hiện) phủ đoạn này, theo thứ tự đầu vào. */
  keys: string[];
  /** Đánh dấu bắt đầu ở đoạn này (để đo vị trí bong bóng ở lề). */
  firstOf: string[];
  /** Chữ thêm vào (thay thế / thêm chữ) hiện ngay sau đoạn này — chỉ khi lớp "sửa" bật. */
  inserts: { key: string; text: string }[];
}

/**
 * Tách bài thành các đoạn liền nhau theo mọi mốc đầu / cuối của đánh dấu (xử lý chồng lấn):
 * mỗi đoạn biết những đánh dấu nào phủ nó. Đánh dấu ngoài phạm vi bài bị bỏ qua.
 */
export function buildSegments(text: string, anns: readonly AnnLike[], layers: Layers = ALL_LAYERS): Segment[] {
  const shown = anns.filter(
    (a) => a.startOffset >= 0 && a.endOffset <= text.length && a.endOffset > a.startOffset && isVisible(a, layers)
  );
  const cuts = new Set<number>([0, text.length]);
  for (const a of shown) {
    cuts.add(a.startOffset);
    cuts.add(a.endOffset);
  }
  const points = [...cuts].sort((x, y) => x - y);
  const segments: Segment[] = [];
  for (let i = 0; i < points.length - 1; i++) {
    const start = points[i];
    const end = points[i + 1];
    if (end <= start) continue;
    const covering = shown.filter((a) => a.startOffset < end && a.endOffset > start);
    segments.push({
      start,
      end,
      text: text.slice(start, end),
      keys: covering.map((a) => a.key),
      firstOf: covering.filter((a) => a.startOffset === start).map((a) => a.key),
      inserts: layers.edit
        ? covering
            .filter((a) => a.endOffset === end && (effectiveEdit(a) === 'REPLACE' || effectiveEdit(a) === 'INSERT'))
            .map((a) => ({ key: a.key, text: a.suggestion ?? '' }))
        : []
    });
  }
  return segments;
}

/** Bỏ khoảng trắng ở hai đầu vùng bôi đen; vùng chỉ toàn khoảng trắng → null. */
export function trimRange(text: string, start: number, end: number): { start: number; end: number } | null {
  let s = Math.max(0, Math.min(start, end));
  let e = Math.min(text.length, Math.max(start, end));
  while (s < e && /\s/.test(text[s])) s++;
  while (e > s && /\s/.test(text[e - 1])) e--;
  return e > s ? { start: s, end: e } : null;
}

/**
 * Xếp bong bóng ở lề phải theo dòng: mỗi cái muốn nằm ngang đoạn của nó (`top`), cái sau bị đẩy xuống nếu
 * chạm cái trước. Giữ thứ tự theo `top` (trùng thì theo thứ tự đầu vào).
 */
export function layoutMargin(
  items: readonly { key: string; top: number; height: number }[],
  gap = 6
): Record<string, number> {
  const sorted = items.map((it, i) => ({ ...it, i })).sort((a, b) => a.top - b.top || a.i - b.i);
  const out: Record<string, number> = {};
  let next = -Infinity;
  for (const it of sorted) {
    const top = Math.max(it.top, next);
    out[it.key] = top;
    next = top + it.height + gap;
  }
  return out;
}

/** Gộp các khoảng dọc (mỗi dòng có đánh dấu) chồng / chạm nhau thành thanh liền ở lề trái. */
export function mergeBars(
  rects: readonly { top: number; bottom: number }[],
  tolerance = 2
): { top: number; bottom: number }[] {
  const sorted = [...rects].filter((r) => r.bottom > r.top).sort((a, b) => a.top - b.top);
  const out: { top: number; bottom: number }[] = [];
  for (const r of sorted) {
    const last = out[out.length - 1];
    if (last && r.top <= last.bottom + tolerance) last.bottom = Math.max(last.bottom, r.bottom);
    else out.push({ ...r });
  }
  return out;
}

// ─── Nhóm lỗi (7 nút chọn nhanh) và thống kê ──────────────────────────────────

export type ErrorCategory =
  'grammar' | 'spelling' | 'vocabulary' | 'collocation' | 'coherence' | 'punctuation' | 'other';

/** 7 nút chọn nhanh khi bôi đen: mỗi nút là một mã lỗi của hệ thống (V9 thêm GR, OTHER). */
export const QUICK_CATEGORIES: readonly { category: ErrorCategory; code: string; label: string }[] = [
  { category: 'grammar', code: 'GR', label: 'Ngữ pháp' },
  { category: 'spelling', code: 'SP', label: 'Chính tả' },
  { category: 'vocabulary', code: 'WW', label: 'Từ vựng' },
  { category: 'collocation', code: 'COL', label: 'Collocation' },
  { category: 'coherence', code: 'LINK', label: 'Liên kết' },
  { category: 'punctuation', code: 'P', label: 'Dấu câu' },
  { category: 'other', code: 'OTHER', label: 'Khác' }
];

export const CATEGORY_LABEL: Record<ErrorCategory, string> = Object.fromEntries(
  QUICK_CATEGORIES.map((c) => [c.category, c.label])
) as Record<ErrorCategory, string>;

/**
 * Nhóm của một mã lỗi: mã của nút chọn nhanh → đúng nhóm đó; mã chi tiết theo nhóm tiêu chí của mã
 * (GRA → Ngữ pháp, LR → Từ vựng, CC → Liên kết; còn lại → Khác).
 */
export function categoryOf(code: string, group: string | null): ErrorCategory {
  const quick = QUICK_CATEGORIES.find((c) => c.code === code.toUpperCase());
  if (quick) return quick.category;
  if (code.toUpperCase() === 'PARA') return 'coherence';
  if (group === 'GRA') return 'grammar';
  if (group === 'LR') return 'vocabulary';
  if (group === 'CC') return 'coherence';
  return 'other';
}

/**
 * Thống kê số lỗi (chỉ đánh dấu kiểu lỗi có mã): theo nhóm và theo tiêu chí. `groupOf` tra nhóm tiêu chí của mã,
 * `criterionOf` cho nhãn tiêu chí của từng đánh dấu (null = chưa gắn).
 */
export function errorStats<T extends AnnLike>(
  anns: readonly T[],
  groupOf: (code: string) => string | null,
  criterionOf: (a: T) => string | null
): { total: number; byCategory: [ErrorCategory, number][]; byCriterion: [string, number][] } {
  const byCategory = new Map<ErrorCategory, number>();
  const byCriterion = new Map<string, number>();
  let total = 0;
  for (const a of anns) {
    if (a.kind !== 'ERROR' || !a.errorCode) continue;
    total++;
    const cat = categoryOf(a.errorCode, groupOf(a.errorCode));
    byCategory.set(cat, (byCategory.get(cat) ?? 0) + 1);
    const crit = criterionOf(a) ?? 'Chưa gắn tiêu chí';
    byCriterion.set(crit, (byCriterion.get(crit) ?? 0) + 1);
  }
  const desc = <K>(m: Map<K, number>) => [...m.entries()].sort((x, y) => y[1] - x[1]);
  return { total, byCategory: desc(byCategory), byCriterion: desc(byCriterion) };
}
