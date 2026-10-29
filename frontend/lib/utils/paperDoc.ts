// ─── Tài liệu paper phía client (docs/12 mục 5.1 – 5.2) ───────────────────────
// Hàm thuần (có test): đếm từ theo mục, chữ thuần cho server, nội dung khởi tạo, quyết định đồng bộ theo số thứ tự.

import { countWords } from './wordCount.ts';

/** Node JSON của trình soạn (dạng ProseMirror) — đủ dùng cho các hàm ở đây. */
export interface DocNode {
  type?: string;
  attrs?: Record<string, unknown>;
  text?: string;
  content?: DocNode[];
}

export interface SectionSpec {
  sectionKey: string;
  title: string;
  required: boolean;
  minWords: number | null;
  maxWords: number | null;
}

export type WordHint = 'empty' | 'short' | 'long' | 'ok';

export interface SectionCount {
  sectionKey: string;
  title: string;
  required: boolean;
  minWords: number | null;
  maxWords: number | null;
  words: number;
  hint: WordHint;
}

/** Chữ của một node, mỗi khối một dòng; công thức (`latex`) không tính là chữ. */
function textOf(node: DocNode): string {
  if (node.type === 'text') return node.text ?? '';
  if (node.type === 'inlineMath' || node.type === 'blockMath') return '';
  const inner = (node.content ?? []).map(textOf);
  const block = node.type !== undefined && !['text', 'hardBreak'].includes(node.type) && node.content !== undefined;
  return block ? inner.join(node.type === 'paragraph' || node.type === 'heading' ? '' : '\n') : inner.join('');
}

/** Nhắc số từ của một mục: trống / thiếu / vượt / đủ — chỉ cảnh báo, không chặn (BR-29). */
export function wordHint(words: number, minWords: number | null, maxWords: number | null): WordHint {
  if (words === 0) return 'empty';
  if (minWords !== null && words < minWords) return 'short';
  if (maxWords !== null && words > maxWords) return 'long';
  return 'ok';
}

/** Số từ từng mục theo thứ tự trong tài liệu, ghép với khung mục của giáo viên để nhắc thiếu / vượt. */
export function sectionWordCounts(doc: DocNode, specs: SectionSpec[]): SectionCount[] {
  const byKey = new Map(specs.map((s) => [s.sectionKey, s]));
  return (doc.content ?? [])
    .filter((n) => n.type === 'paperSection')
    .map((n) => {
      const key = String(n.attrs?.sectionKey ?? '');
      const spec = byKey.get(key);
      const words = countWords(textOf(n));
      return {
        sectionKey: key,
        title: spec?.title ?? String(n.attrs?.title ?? ''),
        required: spec?.required ?? false,
        minWords: spec?.minWords ?? null,
        maxWords: spec?.maxWords ?? null,
        words,
        hint: wordHint(words, spec?.minWords ?? null, spec?.maxWords ?? null)
      };
    });
}

/** Nhãn giới hạn số từ của một mục: "150–600 từ", "≥ 150 từ", "≤ 600 từ"; không giới hạn → null. */
export function wordLimitLabel(minWords: number | null, maxWords: number | null): string | null {
  if (minWords !== null && maxWords !== null) return `${minWords}–${maxWords} từ`;
  if (minWords !== null) return `≥ ${minWords} từ`;
  if (maxWords !== null) return `≤ ${maxWords} từ`;
  return null;
}

/** Chữ thuần gửi kèm snapshot (server đếm từ, xem trực tiếp, đạo văn): tiêu đề mục rồi nội dung. */
export function docText(doc: DocNode): string {
  return (doc.content ?? [])
    .filter((n) => n.type === 'paperSection')
    .map((n) => `${String(n.attrs?.title ?? '')}\n${textOf(n).trim()}`)
    .join('\n\n')
    .trim();
}

/** Nội dung khởi tạo: mỗi mục của khung một `paperSection` có một đoạn trống. */
export function initialDoc(specs: SectionSpec[]): DocNode {
  return {
    type: 'doc',
    content: specs.map((s) => ({
      type: 'paperSection',
      attrs: { sectionKey: s.sectionKey, title: s.title, required: s.required },
      content: [{ type: 'paragraph' }]
    }))
  };
}

/** `refIds` của node trích dẫn lưu dạng "3,7" (thuộc tính Yjs nên dùng chuỗi); bỏ phần không phải số, bỏ trùng. */
export function parseRefIds(raw: unknown): number[] {
  const ids = String(raw ?? '')
    .split(',')
    .map((x) => Number.parseInt(x.trim(), 10))
    .filter((n) => Number.isInteger(n) && n > 0);
  return [...new Set(ids)];
}

/** Các trích dẫn trong tài liệu theo thứ tự xuất hiện, mỗi phần tử là danh sách id tài liệu của một chỗ trích. */
export function citationSequence(doc: DocNode): number[][] {
  const out: number[][] = [];
  const walk = (n: DocNode) => {
    if (n.type === 'citation') {
      const ids = parseRefIds(n.attrs?.refIds);
      if (ids.length) out.push(ids);
      return;
    }
    (n.content ?? []).forEach(walk);
  };
  walk(doc);
  return out;
}

/**
 * Nhận một update có số thứ tự `seq` khi đã có tới `lastSeq`: đã có rồi → bỏ qua; kế tiếp → áp; hở số (lỡ tin lúc
 * mất kết nối) → lấy bù từ REST trước.
 */
export function syncDecision(lastSeq: number, seq: number): 'skip' | 'apply' | 'gap' {
  if (seq <= lastSeq) return 'skip';
  return seq === lastSeq + 1 ? 'apply' : 'gap';
}

export function toBase64(bytes: Uint8Array): string {
  let s = '';
  for (let i = 0; i < bytes.length; i += 0x8000) {
    s += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  }
  return btoa(s);
}

export function fromBase64(b64: string): Uint8Array {
  const s = atob(b64);
  const out = new Uint8Array(s.length);
  for (let i = 0; i < s.length; i++) out[i] = s.charCodeAt(i);
  return out;
}
