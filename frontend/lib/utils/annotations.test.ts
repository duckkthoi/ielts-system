// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
  ALL_LAYERS,
  buildSegments,
  categoryOf,
  effectiveEdit,
  errorStats,
  layoutMargin,
  mergeBars,
  trimRange
} from './annotations.ts';
import type { AnnLike } from './annotations.ts';

const TEXT = 'In recent years, people is increasingly concerned.';
const at = (quote: string) => ({ startOffset: TEXT.indexOf(quote), endOffset: TEXT.indexOf(quote) + quote.length });
const ann = (key: string, quote: string, patch: Partial<AnnLike> = {}): AnnLike => ({
  key,
  ...at(quote),
  kind: 'ERROR',
  errorCode: null,
  comment: null,
  suggestion: null,
  edit: null,
  ...patch
});

test('buildSegments: không có đánh dấu → một đoạn nguyên văn', () => {
  const segs = buildSegments(TEXT, []);
  assert.equal(segs.length, 1);
  assert.equal(segs[0].text, TEXT);
});

test('buildSegments: ghép lại các đoạn luôn ra đúng chữ gốc (chữ của sinh viên không đổi)', () => {
  const anns = [ann('a', 'people is', { errorCode: 'SV' }), ann('b', 'is increasingly', { kind: 'HIGHLIGHT' })];
  assert.equal(
    buildSegments(TEXT, anns)
      .map((s) => s.text)
      .join(''),
    TEXT
  );
});

test('buildSegments: chồng lấn — đoạn giữa thuộc cả hai đánh dấu, đầu mỗi đánh dấu được ghi', () => {
  const segs = buildSegments(TEXT, [
    ann('a', 'people is', { errorCode: 'SV' }),
    ann('b', 'is increasingly', { kind: 'HIGHLIGHT' })
  ]);
  const shared = segs.find((s) => s.text === 'is');
  assert.deepEqual(shared?.keys, ['a', 'b']);
  assert.deepEqual(shared?.firstOf, ['b']);
  assert.deepEqual(segs.find((s) => s.text === 'people ')?.firstOf, ['a']);
});

test('buildSegments: thay thế / thêm chữ → chữ mới đi ngay sau đoạn cuối của đánh dấu; xoá thì không', () => {
  const segs = buildSegments(TEXT, [
    ann('r', 'people is', { edit: 'REPLACE', suggestion: 'people are' }),
    ann('d', 'increasingly', { edit: 'DELETE' }),
    ann('i', 'concerned', { edit: 'INSERT', suggestion: ' about it' })
  ]);
  assert.deepEqual(segs.find((s) => s.text === 'people is')?.inserts, [{ key: 'r', text: 'people are' }]);
  assert.deepEqual(segs.find((s) => s.text === 'increasingly')?.inserts, []);
  assert.deepEqual(segs.find((s) => s.text === 'concerned')?.inserts, [{ key: 'i', text: ' about it' }]);
});

test('buildSegments: tắt lớp → đánh dấu của lớp đó không hiện, tắt lớp "sửa" thì không chèn chữ mới', () => {
  const anns = [ann('h', 'recent', { kind: 'HIGHLIGHT' }), ann('r', 'people is', { suggestion: 'people are' })];
  const noHighlight = buildSegments(TEXT, anns, { ...ALL_LAYERS, highlight: false });
  assert.ok(noHighlight.every((s) => !s.keys.includes('h')));
  const noEdit = buildSegments(TEXT, anns, { ...ALL_LAYERS, edit: false });
  assert.ok(noEdit.every((s) => s.inserts.length === 0 && !s.keys.includes('r')));
});

test('buildSegments: đánh dấu ngoài phạm vi bài bị bỏ qua', () => {
  const segs = buildSegments(TEXT, [{ ...ann('x', 'people'), endOffset: TEXT.length + 5 }]);
  assert.equal(segs.length, 1);
});

test('effectiveEdit: bản ghi cũ chỉ có gợi ý sửa = thay thế', () => {
  assert.equal(effectiveEdit({ kind: 'ERROR', edit: null, suggestion: 'x' }), 'REPLACE');
  assert.equal(effectiveEdit({ kind: 'HIGHLIGHT', edit: null, suggestion: 'x' }), null);
  assert.equal(effectiveEdit({ kind: 'ERROR', edit: 'DELETE', suggestion: null }), 'DELETE');
});

test('trimRange: bỏ khoảng trắng hai đầu; toàn khoảng trắng → null; đảo ngược vẫn đúng', () => {
  assert.deepEqual(trimRange('  ab  ', 0, 6), { start: 2, end: 4 });
  assert.equal(trimRange('a   b', 1, 4), null);
  assert.deepEqual(trimRange('abc', 3, 1), { start: 1, end: 3 });
});

test('layoutMargin: bong bóng gần nhau tự dàn xuống, không chồng; giữ thứ tự theo vị trí', () => {
  const tops = layoutMargin(
    [
      { key: 'b', top: 10, height: 20 },
      { key: 'a', top: 0, height: 20 },
      { key: 'c', top: 200, height: 20 }
    ],
    4
  );
  assert.deepEqual(tops, { a: 0, b: 24, c: 200 });
});

test('mergeBars: các dòng liền / chồng nhau gộp thành một thanh', () => {
  assert.deepEqual(
    mergeBars([
      { top: 30, bottom: 60 },
      { top: 0, bottom: 30 },
      { top: 100, bottom: 130 }
    ]),
    [
      { top: 0, bottom: 60 },
      { top: 100, bottom: 130 }
    ]
  );
});

test('categoryOf: mã chọn nhanh đúng nhóm; mã chi tiết theo nhóm tiêu chí; còn lại → Khác', () => {
  assert.equal(categoryOf('sp', 'LR'), 'spelling');
  assert.equal(categoryOf('VT', 'GRA'), 'grammar');
  assert.equal(categoryOf('WF', 'LR'), 'vocabulary');
  assert.equal(categoryOf('PARA', 'CC'), 'coherence');
  assert.equal(categoryOf('DEV', 'TASK'), 'other');
});

test('errorStats: chỉ đếm lỗi có mã; theo nhóm và theo tiêu chí, nhiều nhất trước', () => {
  const groups: Record<string, string> = { SV: 'GRA', VT: 'GRA', SP: 'LR' };
  const s = errorStats(
    [
      ann('1', 'people', { errorCode: 'SV' }),
      ann('2', 'is', { errorCode: 'VT' }),
      ann('3', 'recent', { errorCode: 'SP' }),
      ann('4', 'years', { kind: 'GOOD' }),
      ann('5', 'In', { comment: 'x' })
    ],
    (c) => groups[c] ?? null,
    (a) => (a.errorCode === 'SP' ? 'LR' : 'GRA')
  );
  assert.equal(s.total, 3);
  assert.deepEqual(s.byCategory, [
    ['grammar', 2],
    ['spelling', 1]
  ]);
  assert.deepEqual(s.byCriterion, [
    ['GRA', 2],
    ['LR', 1]
  ]);
});
