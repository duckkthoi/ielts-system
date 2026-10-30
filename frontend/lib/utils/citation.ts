// ─── Trích dẫn phía client (docs/12 mục 5.3, BR-30) ───────────────────────────
// citation-js: đọc BibTeX → CSL-JSON, định dạng theo style CSL (APA có sẵn trong plugin, IEEE đăng ký từ file chính
// thức). Server chỉ lưu CSL đã lọc; mọi việc định dạng làm ở đây. Có test (node --test).

import { Cite, plugins } from '@citation-js/core';
import '@citation-js/plugin-bibtex';
import '@citation-js/plugin-csl';

import { IEEE_CSL } from '../constants/ieeeCsl.ts';
import type { CitationStyle } from '../types/assignment.ts';
import type { CslItem, CslName, MissingField } from '../types/paper.ts';

const STYLE_ID: Record<CitationStyle, string> = { IEEE: 'ieee', APA: 'apa' };
const LANG = 'en-US';

let registered = false;

function ensureStyles() {
  if (registered) return;
  const csl = plugins.config.get('@csl');
  if (!csl.styles.has('ieee')) csl.styles.add('ieee', IEEE_CSL);
  registered = true;
}

/** Nhãn loại tài liệu hay dùng trong paper ngành IT (giá trị là `type` của CSL). */
export const REFERENCE_TYPES: { value: string; label: string }[] = [
  { value: 'article-journal', label: 'Bài báo tạp chí' },
  { value: 'paper-conference', label: 'Bài hội nghị' },
  { value: 'book', label: 'Sách' },
  { value: 'chapter', label: 'Chương sách' },
  { value: 'thesis', label: 'Luận văn / luận án' },
  { value: 'report', label: 'Báo cáo kỹ thuật' },
  { value: 'webpage', label: 'Trang web' },
  { value: 'software', label: 'Phần mềm / mã nguồn' },
  { value: 'document', label: 'Khác' }
];

const MISSING_LABEL: Record<MissingField, string> = {
  AUTHOR: 'tác giả',
  YEAR: 'năm',
  SOURCE: 'nguồn (tạp chí / hội nghị / NXB / URL)'
};

/** "Thiếu tác giả, năm" — rỗng nếu đủ (BR-30, chỉ cảnh báo). */
export function missingText(missing: MissingField[]): string {
  return missing.length ? `Thiếu ${missing.map((m) => MISSING_LABEL[m]).join(', ')}` : '';
}

// ─── Định dạng ───

/** Một mục theo style của bài, dạng chữ (không HTML). IEEE bỏ số "[1]" vì số thứ tự chỉ có khi đã trích dẫn trong bài. */
export function formatReference(csl: CslItem, style: CitationStyle): string {
  ensureStyles();
  const cite = new Cite({ ...csl, id: 'ref' });
  const text = String(cite.format('bibliography', { format: 'text', style: STYLE_ID[style], lang: LANG })).trim();
  return style === 'IEEE' ? text.replace(/^\[\d+\]\s*/, '') : text;
}

// ─── Nhập BibTeX ───

/**
 * Đọc chuỗi BibTeX thành danh sách CSL (bỏ khoá trích dẫn và dữ liệu nội bộ của citation-js — server tự lọc trường
 * nữa). Sai cú pháp / không có mục nào → Error có message tiếng Việt.
 */
export function parseBibtex(text: string): CslItem[] {
  let data: Record<string, unknown>[];
  try {
    // Ép kiểu đầu vào là BibTeX: chuỗi lạ không bị đoán thành định dạng khác (vd DOI → gọi mạng).
    data = plugins.input.chain(text, {
      forceType: '@biblatex/text',
      target: '@csl/list+object',
      generateGraph: false,
      strict: true,
      maxChainLength: 10
    }) as unknown as Record<string, unknown>[];
  } catch {
    throw new Error('Không đọc được BibTeX. Kiểm tra lại dấu ngoặc { } và dấu phẩy giữa các trường.');
  }
  if (data.length === 0) throw new Error('Không tìm thấy mục nào trong BibTeX.');
  return data.map((d) => {
    const item: CslItem = {};
    for (const [k, v] of Object.entries(d)) {
      if (k !== '_graph' && k !== 'id' && k !== 'citation-key') item[k] = v;
    }
    return item;
  });
}

// ─── Form nhập tay ↔ CSL ───

export interface ReferenceForm {
  type: string;
  title: string;
  /** Mỗi dòng một người: "Họ, Tên" (vd "LeCun, Yann"); không có dấu phẩy thì giữ nguyên cả tên. */
  authors: string;
  year: string;
  containerTitle: string;
  volume: string;
  issue: string;
  page: string;
  publisher: string;
  url: string;
  doi: string;
}

function parseName(line: string): CslName {
  const comma = line.indexOf(',');
  if (comma < 0) return { literal: line };
  const family = line.slice(0, comma).trim();
  const given = line.slice(comma + 1).trim();
  return given ? { family, given } : { literal: family };
}

function nameLine(n: CslName): string {
  if (n.family) return n.given ? `${n.family}, ${n.given}` : n.family;
  return n.literal ?? n.given ?? '';
}

/** Form → CSL: bỏ trường trống, năm thành `issued.date-parts`. */
export function cslFromForm(f: ReferenceForm): CslItem {
  const csl: CslItem = { type: f.type || 'document', title: f.title.trim() };
  const authors = f.authors
    .split('\n')
    .map((l) => l.trim())
    .filter(Boolean)
    .map(parseName);
  if (authors.length) csl.author = authors;
  const year = Number.parseInt(f.year.trim(), 10);
  if (Number.isFinite(year)) csl.issued = { 'date-parts': [[year]] };
  const text: [keyof ReferenceForm, string][] = [
    ['containerTitle', 'container-title'],
    ['volume', 'volume'],
    ['issue', 'issue'],
    ['page', 'page'],
    ['publisher', 'publisher'],
    ['url', 'URL'],
    ['doi', 'DOI']
  ];
  for (const [from, to] of text) {
    const v = f[from].trim();
    if (v) csl[to] = v;
  }
  return csl;
}

/** CSL → form để sửa. Trường không có trong form (vd biên tập viên) giữ lại khi lưu nhờ `mergeForm`. */
export function formFromCsl(csl: CslItem): ReferenceForm {
  const year = csl.issued?.['date-parts']?.[0]?.[0];
  const str = (v: unknown) => (v === undefined || v === null ? '' : String(v));
  return {
    type: csl.type ?? 'document',
    title: csl.title ?? '',
    authors: (csl.author ?? []).map(nameLine).filter(Boolean).join('\n'),
    year: str(year ?? csl.issued?.literal ?? ''),
    containerTitle: str(csl['container-title']),
    volume: str(csl.volume),
    issue: str(csl.issue),
    page: str(csl.page),
    publisher: str(csl.publisher),
    url: str(csl.URL),
    doi: str(csl.DOI)
  };
}

/** Lưu form đè lên bản ghi cũ: trường form quản lý lấy theo form, trường khác (biên tập viên, ISSN…) giữ nguyên. */
export function mergeForm(original: CslItem, f: ReferenceForm): CslItem {
  const managed = [
    'type',
    'title',
    'author',
    'issued',
    'container-title',
    'volume',
    'issue',
    'page',
    'publisher',
    'URL',
    'DOI'
  ];
  const kept: CslItem = {};
  for (const [k, v] of Object.entries(original)) {
    if (!managed.includes(k)) kept[k] = v;
  }
  return { ...kept, ...cslFromForm(f) };
}

// ─── Trích dẫn trong bài + danh mục (BR-30) ───
// IEEE: đánh số theo thứ tự xuất hiện lần đầu, danh mục theo số. APA: (Tác giả, năm), danh mục xếp ABC (citeproc).
// Tài liệu không còn trong thư viện (đã xoá / chưa tải) hiện "[?]" / "(?)" và không vào danh mục.

/** Id tài liệu theo thứ tự xuất hiện lần đầu, chỉ những id có trong thư viện. */
export function citationOrder(sequence: number[][], known: Map<number, CslItem>): number[] {
  const seen = new Set<number>();
  for (const ids of sequence) {
    for (const id of ids) if (known.has(id)) seen.add(id);
  }
  return [...seen];
}

/** "[1], [2]" — từ 3 số liên tiếp trở lên gộp thành "[1]–[3]" (theo hướng dẫn IEEE). */
function ieeeLabel(numbers: number[], unknown: boolean): string {
  const sorted = [...new Set(numbers)].sort((a, b) => a - b);
  const parts: string[] = [];
  for (let i = 0; i < sorted.length;) {
    let j = i;
    while (j + 1 < sorted.length && sorted[j + 1] === sorted[j] + 1) j++;
    if (j - i >= 2) parts.push(`[${sorted[i]}]–[${sorted[j]}]`);
    else for (let k = i; k <= j; k++) parts.push(`[${sorted[k]}]`);
    i = j + 1;
  }
  if (unknown) parts.push('[?]');
  return parts.join(', ');
}

function citeOf(ids: number[], known: Map<number, CslItem>) {
  return new Cite(ids.map((id) => ({ ...known.get(id), id: String(id) })));
}

/** Nhãn của từng chỗ trích dẫn (cùng thứ tự với `sequence`). */
export function citationLabels(sequence: number[][], known: Map<number, CslItem>, style: CitationStyle): string[] {
  if (style === 'IEEE') {
    const number = new Map(citationOrder(sequence, known).map((id, i) => [id, i + 1]));
    return sequence.map((ids) =>
      ieeeLabel(
        ids.filter((id) => number.has(id)).map((id) => number.get(id) as number),
        ids.some((id) => !number.has(id))
      )
    );
  }
  ensureStyles();
  return sequence.map((ids) => {
    const found = ids.filter((id) => known.has(id));
    if (found.length === 0) return '(?)';
    const label = String(
      citeOf(found, known).format('citation', {
        format: 'text',
        style: STYLE_ID.APA,
        lang: LANG,
        entry: found.map(String),
        citationsPre: [],
        citationsPost: []
      })
    );
    return found.length < ids.length ? label.replace(/\)$/, '; ?)') : label;
  });
}

/** Danh mục tài liệu tham khảo của các tài liệu được trích dẫn: IEEE theo số thứ tự, APA theo ABC. */
export function bibliographyEntries(
  sequence: number[][],
  known: Map<number, CslItem>,
  style: CitationStyle
): { id: number; text: string }[] {
  const order = citationOrder(sequence, known);
  if (order.length === 0) return [];
  if (style === 'IEEE') {
    return order.map((id, i) => ({ id, text: `[${i + 1}] ${formatReference(known.get(id) as CslItem, 'IEEE')}` }));
  }
  ensureStyles();
  const entries = citeOf(order, known).format('bibliography', {
    format: 'text',
    style: STYLE_ID.APA,
    lang: LANG,
    asEntryArray: true
  }) as unknown as [string, string][];
  return entries.map(([id, text]) => ({ id: Number(id), text: text.trim() }));
}
