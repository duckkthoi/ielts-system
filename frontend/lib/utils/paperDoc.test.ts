// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
  citationSequence,
  docText,
  docTextPositions,
  fromBase64,
  initialDoc,
  offsetsToRange,
  parseRefIds,
  rangeToOffsets,
  sectionWordCounts,
  syncDecision,
  toBase64,
  wordHint,
  wordLimitLabel
} from './paperDoc.ts';
import type { DocNode, PmNodeLike, SectionSpec } from './paperDoc.ts';

const specs: SectionSpec[] = [
  { sectionKey: 'abstract', title: 'Abstract', required: true, minWords: 3, maxWords: 5 },
  { sectionKey: 'method', title: 'Method', required: true, minWords: null, maxWords: null }
];

const para = (text: string): DocNode => ({ type: 'paragraph', content: [{ type: 'text', text }] });

const doc: DocNode = {
  type: 'doc',
  content: [
    {
      type: 'paperSection',
      attrs: { sectionKey: 'abstract', title: 'Abstract' },
      content: [para('We propose a system.')]
    },
    {
      type: 'paperSection',
      attrs: { sectionKey: 'method', title: 'Method' },
      content: [
        para('Train the model'),
        { type: 'blockMath', attrs: { latex: 'E = mc^2' } },
        { type: 'bulletList', content: [{ type: 'listItem', content: [para('step one')] }] }
      ]
    }
  ]
};

test('sectionWordCounts: đếm theo từng mục, công thức không tính, ghép khung mục để nhắc', () => {
  const counts = sectionWordCounts(doc, specs);
  assert.deepEqual(
    counts.map((c) => [c.sectionKey, c.words, c.hint]),
    [
      ['abstract', 4, 'ok'],
      ['method', 5, 'ok']
    ]
  );
});

test('sectionWordCounts: mang theo bắt buộc + giới hạn của khung; mục lạ (không có trong khung) thì không giới hạn', () => {
  const withExtra: DocNode = {
    type: 'doc',
    content: [...(doc.content ?? []), { type: 'paperSection', attrs: { sectionKey: 'old', title: 'Cũ' }, content: [] }]
  };
  const counts = sectionWordCounts(withExtra, specs);
  assert.deepEqual(
    counts.map((c) => [c.sectionKey, c.title, c.required, c.minWords, c.maxWords, c.hint]),
    [
      ['abstract', 'Abstract', true, 3, 5, 'ok'],
      ['method', 'Method', true, null, null, 'ok'],
      ['old', 'Cũ', false, null, null, 'empty']
    ]
  );
});

test('wordLimitLabel: khoảng / tối thiểu / tối đa / không giới hạn', () => {
  assert.equal(wordLimitLabel(150, 600), '150–600 từ');
  assert.equal(wordLimitLabel(150, null), '≥ 150 từ');
  assert.equal(wordLimitLabel(null, 600), '≤ 600 từ');
  assert.equal(wordLimitLabel(null, null), null);
});

test('wordHint: trống / thiếu / vượt / đủ (chỉ nhắc, không chặn — BR-29)', () => {
  assert.equal(wordHint(0, 3, 5), 'empty');
  assert.equal(wordHint(2, 3, 5), 'short');
  assert.equal(wordHint(6, 3, 5), 'long');
  assert.equal(wordHint(4, 3, 5), 'ok');
  assert.equal(wordHint(4, null, null), 'ok');
});

test('docText: tiêu đề mục + nội dung, các khối cách dòng', () => {
  assert.equal(docText(doc), 'Abstract\nWe propose a system.\n\nMethod\nTrain the model\n\nstep one');
});

const LEAVES = new Set(['blockMath', 'inlineMath', 'hardBreak', 'citation', 'image']);

/** Dựng node kiểu ProseMirror từ JSON (kích thước: chữ = độ dài, lá = 1, còn lại = 2 + các con). */
function pm(n: DocNode): PmNodeLike {
  const kids = (n.content ?? []).map(pm);
  const isText = n.type === 'text';
  const leaf = LEAVES.has(n.type ?? '');
  return {
    type: { name: n.type ?? '' },
    isText,
    text: n.text,
    attrs: n.attrs ?? {},
    childCount: kids.length,
    child: (i) => kids[i],
    nodeSize: isText ? (n.text ?? '').length : leaf ? 1 : 2 + kids.reduce((sum, k) => sum + k.nodeSize, 0)
  };
}

test('docTextPositions: chữ giống hệt docText; mỗi ký tự chữ có vị trí ProseMirror, tiêu đề / xuống dòng thì -1', () => {
  const { text, positions } = docTextPositions(pm(doc));
  assert.equal(text, docText(doc));
  assert.equal(positions.length, text.length);
  // doc(0) > section(1) > paragraph(2) > chữ bắt đầu ở 2
  assert.equal(positions[text.indexOf('We')], 2);
  assert.equal(positions[0], -1); // "Abstract" là tiêu đề (thuộc tính), không có vị trí
  // mục 2: section mở ở 24 (= 0 + kích thước mục 1 = 24), đoạn ở 25, chữ ở 26
  assert.equal(positions[text.indexOf('Train')], 26);
  assert.equal(positions[text.indexOf('step one')] > positions[text.indexOf('model')], true);
});

test('docTextPositions: khoảng trắng đầu / cuối mục bị cắt như docText', () => {
  const padded: DocNode = {
    type: 'doc',
    content: [{ type: 'paperSection', attrs: { title: 'A' }, content: [para('  hi  '), { type: 'paragraph' }] }]
  };
  const { text, positions } = docTextPositions(pm(padded));
  assert.equal(text, docText(padded));
  assert.equal(text, 'A\nhi');
  assert.equal(positions[2], 4); // "h": section 0 → đoạn 1 → chữ 2, bỏ 2 dấu cách
});

test('offsetsToRange / rangeToOffsets: đi về giữ nguyên; khoảng chỉ có tiêu đề → null', () => {
  const { text, positions } = docTextPositions(pm(doc));
  const start = text.indexOf('propose');
  const range = offsetsToRange(positions, start, start + 'propose'.length);
  assert.deepEqual(range, { from: 5, to: 12 });
  assert.deepEqual(rangeToOffsets(positions, range!.from, range!.to), { start, end: start + 7 });
  assert.equal(offsetsToRange(positions, 0, 8), null);
  // bôi đen qua hai mục: lấy từ chữ đầu tới chữ cuối nằm trong vùng
  const across = rangeToOffsets(positions, 2, positions[text.indexOf('Train')] + 5);
  assert.equal(text.slice(across!.start, across!.end), 'We propose a system.\n\nMethod\nTrain');
});

test('initialDoc: mỗi mục một paperSection có một đoạn trống, đúng thứ tự khung', () => {
  const init = initialDoc(specs);
  assert.deepEqual(
    init.content?.map((n) => [n.type, n.attrs?.sectionKey, n.content?.[0].type]),
    [
      ['paperSection', 'abstract', 'paragraph'],
      ['paperSection', 'method', 'paragraph']
    ]
  );
});

test('syncDecision: đã có → bỏ qua; kế tiếp → áp; hở số → lấy bù', () => {
  assert.equal(syncDecision(5, 5), 'skip');
  assert.equal(syncDecision(5, 3), 'skip');
  assert.equal(syncDecision(5, 6), 'apply');
  assert.equal(syncDecision(5, 8), 'gap');
});

test('base64: đi về giữ nguyên byte, kể cả mảng lớn', () => {
  const bytes = new Uint8Array(100_000).map((_, i) => i % 256);
  assert.deepEqual(fromBase64(toBase64(bytes)), bytes);
  assert.equal(toBase64(new Uint8Array([1, 2, 3])), 'AQID');
});

test('parseRefIds: chuỗi "3,7" → số, bỏ rác và trùng', () => {
  assert.deepEqual(parseRefIds('3, 7,3'), [3, 7]);
  assert.deepEqual(parseRefIds('a,0,-2,5'), [5]);
  assert.deepEqual(parseRefIds(undefined), []);
});

test('citationSequence: trích dẫn theo thứ tự xuất hiện, kể cả trong danh sách / bảng; bỏ trích dẫn rỗng', () => {
  const cite = (refIds: string): DocNode => ({ type: 'citation', attrs: { refIds } });
  const withCites: DocNode = {
    type: 'doc',
    content: [
      {
        type: 'paperSection',
        attrs: { sectionKey: 'a' },
        content: [{ type: 'paragraph', content: [cite('5'), cite('')] }]
      },
      {
        type: 'paperSection',
        attrs: { sectionKey: 'b' },
        content: [
          {
            type: 'bulletList',
            content: [{ type: 'listItem', content: [{ type: 'paragraph', content: [cite('2,5')] }] }]
          }
        ]
      }
    ]
  };
  assert.deepEqual(citationSequence(withCites), [[5], [2, 5]]);
});
