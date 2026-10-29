// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
  citationSequence,
  docText,
  fromBase64,
  initialDoc,
  parseRefIds,
  sectionWordCounts,
  syncDecision,
  toBase64,
  wordHint,
  wordLimitLabel
} from './paperDoc.ts';
import type { DocNode, SectionSpec } from './paperDoc.ts';

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
