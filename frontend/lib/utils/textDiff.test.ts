// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { diffWords, fixSummary, guessResolution, mapOldPos, mapOldRange, tokenize, withContext } from './textDiff.ts';
import type { DiffOp, OldError } from './textDiff.ts';

const pick = (ops: DiffOp[]) => ops.map((o) => [o.type, o.text]);
/** Ghép lại hai bài từ diff — phải ra đúng nguyên văn. */
const rebuild = (ops: DiffOp[]) => ({
  old: ops
    .filter((o) => o.type !== 'added')
    .map((o) => o.text)
    .join(''),
  new: ops
    .filter((o) => o.type !== 'removed')
    .map((o) => o.text)
    .join('')
});

test('tokenize: từ có nháy / gạch nối là một từ, dấu câu và khoảng trắng tách riêng', () => {
  assert.deepEqual(
    tokenize("I don't like well-known  ideas.").map((t) => t.text),
    ['I', ' ', "don't", ' ', 'like', ' ', 'well-known', '  ', 'ideas', '.']
  );
});

test('diffWords: hai bài giống nhau → một đoạn giống', () => {
  assert.deepEqual(pick(diffWords('Same text.', 'Same text.')), [['equal', 'Same text.']]);
});

test('diffWords: thay cụm từ → một đoạn bỏ rồi một đoạn thêm (khoảng trắng giữa không bị vụn)', () => {
  const ops = diffWords('University should cost less.', 'University should be free for everyone.');
  assert.deepEqual(pick(ops), [
    ['equal', 'University should '],
    ['removed', 'cost less'],
    ['added', 'be free for everyone'],
    ['equal', '.']
  ]);
});

test('diffWords: offset hai phía khớp nguyên văn, ghép lại ra đúng hai bài', () => {
  const a = 'In recent years, people is concerned. Technology have changed life.';
  const b = 'Recently, people are concerned. Technology has changed our life a lot.';
  const ops = diffWords(a, b);
  assert.deepEqual(rebuild(ops), { old: a, new: b });
  for (const o of ops) {
    if (o.type !== 'added') assert.equal(a.slice(o.oldStart, o.oldEnd), o.text);
    if (o.type !== 'removed') assert.equal(b.slice(o.newStart, o.newEnd), o.text);
  }
});

test('diffWords: bài rỗng một phía', () => {
  assert.deepEqual(pick(diffWords('', 'New essay.')), [['added', 'New essay.']]);
  assert.deepEqual(pick(diffWords('Old essay.', '')), [['removed', 'Old essay.']]);
  assert.deepEqual(diffWords('', ''), []);
});

test('mapOldPos / mapOldRange: đoạn không đổi và đoạn đã thay', () => {
  const a = 'people is happy';
  const b = 'many people are happy';
  const ops = diffWords(a, b);
  assert.equal(mapOldPos(ops, a.indexOf('happy')), b.indexOf('happy'));
  assert.deepEqual(mapOldRange(ops, a.indexOf('happy'), a.length), { unchanged: true, text: 'happy' });
  const r = mapOldRange(ops, 0, 'people is'.length);
  assert.equal(r.unchanged, false);
  assert.match(r.text, /people are/);
});

test('withContext: lấy tối đa 3 từ mỗi bên', () => {
  const t = 'one two three four five six seven eight';
  assert.equal(withContext(t, t.indexOf('five'), t.indexOf('five') + 4), 'two three four five six seven eight');
  assert.equal(withContext(t, 0, 3), 'one two three four');
});

// ─── guessResolution ──────────────────────────────────────────────────────────

const OLD = 'In recent years, people is increasingly concerned about recieve the environment in market.';
const err = (quote: string, patch: Partial<OldError> = {}): OldError => ({
  startOffset: OLD.indexOf(quote),
  endOffset: OLD.indexOf(quote) + quote.length,
  kind: 'ERROR',
  suggestion: null,
  edit: null,
  ...patch
});
const guess = (newText: string, e: OldError) => guessResolution(OLD, newText, diffWords(OLD, newText), e);

test('guessResolution: sửa đúng gợi ý → Đã sửa', () => {
  const next = OLD.replace('people is', 'people are');
  assert.equal(guess(next, err('people is', { suggestion: 'people are', edit: 'REPLACE' })), 'FIXED');
  // bản ghi cũ: có gợi ý mà không có kiểu sửa = thay thế
  assert.equal(guess(next, err('people is', { suggestion: 'people are' })), 'FIXED');
});

test('guessResolution: đoạn trích còn nguyên → Chưa sửa (kể cả khi câu khác đã đổi / đoạn bị dời)', () => {
  const e = err('people is', { suggestion: 'people are', edit: 'REPLACE' });
  assert.equal(guess(OLD, e), 'NOT_FIXED');
  assert.equal(guess(OLD.replace('recieve', 'receive'), e), 'NOT_FIXED');
  const moved = 'The environment matters. In recent years, people is increasingly concerned.';
  assert.equal(guess(moved, e), 'NOT_FIXED');
});

test('guessResolution: đổi nhưng khác gợi ý → Đổi khác gợi ý', () => {
  const next = OLD.replace('people is', 'humans were');
  assert.equal(guess(next, err('people is', { suggestion: 'people are', edit: 'REPLACE' })), 'CHANGED');
});

test('guessResolution: lỗi chỉ có mã / xoá chữ — đoạn trích không còn → Đã sửa', () => {
  assert.equal(guess(OLD.replace('recieve', 'receive'), err('recieve')), 'FIXED');
  assert.equal(guess(OLD.replace('increasingly ', ''), err('increasingly', { edit: 'DELETE' })), 'FIXED');
  assert.equal(guess(OLD, err('increasingly', { edit: 'DELETE' })), 'NOT_FIXED');
});

test('guessResolution: thêm chữ — có chữ cần thêm ngay sau đoạn trích → Đã sửa', () => {
  const e = err('in', { suggestion: 'the', edit: 'INSERT', startOffset: OLD.indexOf('in market') });
  e.endOffset = e.startOffset + 2;
  assert.equal(guess(OLD.replace('in market', 'in the market'), e), 'FIXED');
  assert.equal(guess(OLD, e), 'NOT_FIXED');
  assert.equal(guess(OLD.replace('in market', 'in a market'), e), 'CHANGED');
});

// ─── fixSummary ───────────────────────────────────────────────────────────────

test('fixSummary: đếm Đã sửa X/Y theo nhóm lỗi và tiêu chí, bỏ lỗi không có mã', () => {
  const s = fixSummary(
    [
      { errorCode: 'SV', resolution: 'FIXED', criterionLabel: 'GRA' },
      { errorCode: 'GR', resolution: 'NOT_FIXED', criterionLabel: 'GRA' },
      { errorCode: 'SP', resolution: 'FIXED', criterionLabel: 'LR' },
      { errorCode: 'WW', resolution: 'CHANGED', criterionLabel: null },
      { errorCode: null, resolution: 'FIXED', criterionLabel: null }
    ],
    (code) => (code === 'SV' ? 'GRA' : null)
  );
  assert.deepEqual([s.total, s.fixed, s.changed, s.notFixed], [4, 2, 1, 1]);
  assert.deepEqual(s.byCategory[0], ['grammar', { fixed: 1, total: 2 }]);
  assert.deepEqual(Object.fromEntries(s.byCriterion), {
    GRA: { fixed: 1, total: 2 },
    LR: { fixed: 1, total: 1 },
    'Chưa gắn tiêu chí': { fixed: 0, total: 1 }
  });
});
