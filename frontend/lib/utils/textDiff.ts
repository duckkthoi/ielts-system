// ─── So sánh 2 lần nộp (F5b, docs/08 mục 4.3) ─────────────────────────────────
// Hàm thuần, không import alias để `node --test` chạy trực tiếp. Diff theo từ bằng LCS tự viết (không thêm thư viện);
// offset theo đơn vị UTF-16 (String.length của JS) như server.

import { categoryOf, effectiveEdit } from './annotations.ts';
import type { AnnEdit, AnnKind, ErrorCategory } from './annotations.ts';

export type DiffType = 'equal' | 'added' | 'removed';

export interface DiffOp {
  type: DiffType;
  text: string;
  /** Vị trí trong bài cũ; đoạn thêm có oldStart = oldEnd = chỗ chèn. */
  oldStart: number;
  oldEnd: number;
  /** Vị trí trong bài mới; đoạn bỏ có newStart = newEnd = chỗ bị bỏ. */
  newStart: number;
  newEnd: number;
}

interface Token {
  text: string;
  start: number;
}

/** Quá số ô này (vd 2 bài ~2000 từ) thì không chạy LCS nữa: coi phần giữa là bỏ hết rồi thêm mới. */
const MAX_CELLS = 4_000_000;

/** Tách thành từ (gồm từ có nháy / gạch nối: don't, well-known), khoảng trắng, và từng dấu câu. */
export function tokenize(text: string): Token[] {
  const out: Token[] = [];
  for (const m of text.matchAll(/\s+|[\p{L}\p{N}]+(?:['’-][\p{L}\p{N}]+)*|\S/gu)) {
    out.push({ text: m[0], start: m.index });
  }
  return out;
}

/**
 * Diff theo từ: các đoạn giống / thêm / bỏ nối liền nhau, phủ hết cả hai bài. Mỗi chỗ thay đổi gộp thành một đoạn
 * bỏ rồi một đoạn thêm (khoảng trắng kẹt giữa hai chỗ đổi được gộp vào, để "cost less" → "be free" không bị vụn).
 */
export function diffWords(oldText: string, newText: string): DiffOp[] {
  const a = tokenize(oldText);
  const b = tokenize(newText);
  let pre = 0;
  while (pre < a.length && pre < b.length && a[pre].text === b[pre].text) pre++;
  let suf = 0;
  while (suf < a.length - pre && suf < b.length - pre && a[a.length - 1 - suf].text === b[b.length - 1 - suf].text) {
    suf++;
  }
  const midA = a.slice(pre, a.length - suf);
  const midB = b.slice(pre, b.length - suf);

  // Mỗi token: 'equal' (kèm token bên mới), 'removed' hoặc 'added'.
  const steps: { type: DiffType; a?: Token; b?: Token }[] = [];
  for (let i = 0; i < pre; i++) steps.push({ type: 'equal', a: a[i], b: b[i] });
  const n = midA.length;
  const m = midB.length;
  if ((n + 1) * (m + 1) > MAX_CELLS) {
    for (const t of midA) steps.push({ type: 'removed', a: t });
    for (const t of midB) steps.push({ type: 'added', b: t });
  } else {
    // dp[i][j] = độ dài LCS của midA[i..] và midB[j..]
    const w = m + 1;
    const dp = new Uint32Array((n + 1) * w);
    for (let i = n - 1; i >= 0; i--) {
      for (let j = m - 1; j >= 0; j--) {
        dp[i * w + j] =
          midA[i].text === midB[j].text
            ? dp[(i + 1) * w + j + 1] + 1
            : Math.max(dp[(i + 1) * w + j], dp[i * w + j + 1]);
      }
    }
    let i = 0;
    let j = 0;
    while (i < n || j < m) {
      if (i < n && j < m && midA[i].text === midB[j].text) {
        steps.push({ type: 'equal', a: midA[i++], b: midB[j++] });
      } else if (j >= m || (i < n && dp[(i + 1) * w + j] >= dp[i * w + j + 1])) {
        steps.push({ type: 'removed', a: midA[i++] });
      } else {
        steps.push({ type: 'added', b: midB[j++] });
      }
    }
  }
  for (let k = suf; k > 0; k--) steps.push({ type: 'equal', a: a[a.length - k], b: b[b.length - k] });

  // Khoảng trắng giống nhau nằm giữa hai chỗ đổi → coi là một phần của chỗ đổi.
  const isChange = (k: number) => k >= 0 && k < steps.length && steps[k].type !== 'equal';
  const absorbed = steps.map((s, k) => {
    if (s.type !== 'equal' || !/^\s+$/.test(s.a!.text)) return false;
    let l = k - 1;
    while (l >= 0 && steps[l].type === 'equal' && /^\s+$/.test(steps[l].a!.text)) l--;
    let r = k + 1;
    while (r < steps.length && steps[r].type === 'equal' && /^\s+$/.test(steps[r].a!.text)) r++;
    return isChange(l) && isChange(r);
  });

  const ops: DiffOp[] = [];
  let oldPos = 0;
  let newPos = 0;
  let k = 0;
  while (k < steps.length) {
    if (steps[k].type === 'equal' && !absorbed[k]) {
      let text = '';
      while (k < steps.length && steps[k].type === 'equal' && !absorbed[k]) text += steps[k++].a!.text;
      ops.push({
        type: 'equal',
        text,
        oldStart: oldPos,
        oldEnd: oldPos + text.length,
        newStart: newPos,
        newEnd: newPos + text.length
      });
      oldPos += text.length;
      newPos += text.length;
      continue;
    }
    let removed = '';
    let added = '';
    while (k < steps.length && (steps[k].type !== 'equal' || absorbed[k])) {
      const s = steps[k++];
      if (s.type === 'equal') {
        removed += s.a!.text;
        added += s.b!.text;
      } else if (s.type === 'removed') removed += s.a!.text;
      else added += s.b!.text;
    }
    if (removed) {
      ops.push({
        type: 'removed',
        text: removed,
        oldStart: oldPos,
        oldEnd: oldPos + removed.length,
        newStart: newPos,
        newEnd: newPos
      });
      oldPos += removed.length;
    }
    if (added) {
      ops.push({
        type: 'added',
        text: added,
        oldStart: oldPos,
        oldEnd: oldPos,
        newStart: newPos,
        newEnd: newPos + added.length
      });
      newPos += added.length;
    }
  }
  return ops;
}

/**
 * Vị trí trong bài mới tương ứng một vị trí của bài cũ. Cuối một đoạn giống → ngay trước chữ chèn sau đó;
 * giữa đoạn bị bỏ → chỗ đoạn đó từng nằm.
 */
export function mapOldPos(ops: readonly DiffOp[], pos: number): number {
  for (const op of ops) {
    if (op.type === 'equal' && pos >= op.oldStart && pos <= op.oldEnd) return op.newStart + (pos - op.oldStart);
    if (op.type === 'removed' && pos >= op.oldStart && pos < op.oldEnd) return op.newStart;
  }
  return ops.length ? ops[ops.length - 1].newEnd : 0;
}

/**
 * Đoạn [start, end) của bài cũ ở bài mới: `unchanged` khi không chữ nào trong đoạn bị bỏ và không chèn gì vào giữa;
 * `text` là chữ bên mới thay cho đoạn (gồm cả chữ chèn sát hai đầu).
 */
export function mapOldRange(ops: readonly DiffOp[], start: number, end: number): { unchanged: boolean; text: string } {
  let unchanged = true;
  let text = '';
  for (const op of ops) {
    if (op.type === 'added') {
      if (op.oldStart > start && op.oldStart < end) unchanged = false;
      if (op.oldStart >= start && op.oldStart <= end) text += op.text;
    } else if (op.oldStart < end && op.oldEnd > start) {
      if (op.type === 'removed') unchanged = false;
      else text += op.text.slice(Math.max(0, start - op.oldStart), Math.min(op.text.length, end - op.oldStart));
    }
  }
  return { unchanged, text };
}

// ─── Đoán lỗi của lần cũ đã sửa chưa ─────────────────────────────────────────

export type Resolution = 'FIXED' | 'NOT_FIXED' | 'CHANGED';

export interface OldError {
  startOffset: number;
  endOffset: number;
  kind: AnnKind;
  suggestion: string | null;
  edit: AnnEdit | null;
}

/** Số từ lấy thêm mỗi bên đoạn trích khi tìm lại đoạn trích ở bài mới. */
const CONTEXT_WORDS = 3;

const norm = (s: string) => s.replace(/\s+/g, ' ').trim().toLowerCase();

/** Đoạn trích kèm tối đa `CONTEXT_WORDS` từ trước / sau, lấy từ bài cũ (bất biến). */
export function withContext(text: string, start: number, end: number, words = CONTEXT_WORDS): string {
  let s = start;
  for (let w = 0; w < words && s > 0; w++) {
    while (s > 0 && /\s/.test(text[s - 1])) s--;
    while (s > 0 && !/\s/.test(text[s - 1])) s--;
  }
  let e = end;
  for (let w = 0; w < words && e < text.length; w++) {
    while (e < text.length && /\s/.test(text[e])) e++;
    while (e < text.length && !/\s/.test(text[e])) e++;
  }
  return text.slice(s, e);
}

/**
 * Đoán (docs/08 mục 4.3): đoạn trích còn nguyên kèm vài chữ trước / sau ở bài mới, hoặc diff cho thấy không đổi →
 * Chưa sửa. Đã đổi: có gợi ý thay / thêm chữ thì đúng gợi ý → Đã sửa, khác → Đổi khác gợi ý; xoá / không có gợi ý
 * → Đã sửa (đoạn trích không còn). Thêm chữ: xem chữ ngay sau đoạn trích ở bài mới.
 */
export function guessResolution(oldText: string, newText: string, ops: readonly DiffOp[], e: OldError): Resolution {
  const edit = effectiveEdit({ kind: e.kind, edit: e.edit, suggestion: e.suggestion });
  const suggestion = e.suggestion ? norm(e.suggestion) : '';
  const range = mapOldRange(ops, e.startOffset, e.endOffset);

  if (edit === 'INSERT') {
    if (!range.unchanged) return 'CHANGED';
    const after = norm(newText.slice(mapOldPos(ops, e.endOffset)));
    if (suggestion && after.startsWith(suggestion)) return 'FIXED';
    const oldAfter = norm(oldText.slice(e.endOffset));
    const nextWord = (s: string) => s.split(' ')[0] ?? '';
    return nextWord(after) === nextWord(oldAfter) ? 'NOT_FIXED' : 'CHANGED';
  }

  const context = norm(withContext(oldText, e.startOffset, e.endOffset));
  if (range.unchanged || (context && norm(newText).includes(context))) return 'NOT_FIXED';
  if (edit === 'REPLACE' && suggestion) return norm(range.text).includes(suggestion) ? 'FIXED' : 'CHANGED';
  return 'FIXED';
}

// ─── Tóm tắt "Đã sửa X/Y" ─────────────────────────────────────────────────────

export interface ResolvedError {
  errorCode: string | null;
  resolution: Resolution;
  criterionLabel: string | null;
}

export interface FixCount {
  fixed: number;
  total: number;
}

/** Chỉ tính lỗi có mã (như thống kê lỗi); nhóm và tiêu chí xếp theo số lỗi nhiều nhất trước. */
export function fixSummary(
  errors: readonly ResolvedError[],
  groupOf: (code: string) => string | null
): {
  total: number;
  fixed: number;
  changed: number;
  notFixed: number;
  byCategory: [ErrorCategory, FixCount][];
  byCriterion: [string, FixCount][];
} {
  const byCategory = new Map<ErrorCategory, FixCount>();
  const byCriterion = new Map<string, FixCount>();
  const count = { total: 0, fixed: 0, changed: 0, notFixed: 0 };
  const bump = <K>(m: Map<K, FixCount>, k: K, fixed: boolean) => {
    const c = m.get(k) ?? { fixed: 0, total: 0 };
    c.total++;
    if (fixed) c.fixed++;
    m.set(k, c);
  };
  for (const e of errors) {
    if (!e.errorCode) continue;
    const fixed = e.resolution === 'FIXED';
    count.total++;
    if (fixed) count.fixed++;
    else if (e.resolution === 'CHANGED') count.changed++;
    else count.notFixed++;
    bump(byCategory, categoryOf(e.errorCode, groupOf(e.errorCode)), fixed);
    bump(byCriterion, e.criterionLabel ?? 'Chưa gắn tiêu chí', fixed);
  }
  const desc = <K>(m: Map<K, FixCount>) => [...m.entries()].sort((x, y) => y[1].total - x[1].total);
  return { ...count, byCategory: desc(byCategory), byCriterion: desc(byCriterion) };
}
