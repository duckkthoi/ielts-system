// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
  adjustmentOptions,
  clampScore,
  formatScore,
  levelOfScore,
  memberScore,
  paperScore,
  partBandRaw,
  roundBand,
  scoreCap,
  scoreForLevel,
  scoreOptions
} from './grading.ts';

test('roundBand (BR-12): làm tròn .25 / .75 giống BandCalculator của server', () => {
  assert.equal(roundBand(6.125), 6);
  assert.equal(roundBand(6.25), 6.5);
  assert.equal(roundBand(6.583), 6.5);
  assert.equal(roundBand(6.75), 7);
  assert.equal(roundBand(6.7499999999), 7);
  assert.equal(roundBand(null), null);
});

test('partBandRaw (BR-12): trung bình có trọng số; còn tiêu chí chưa chấm → null', () => {
  assert.equal(
    partBandRaw([
      { score: 6, weight: 1 },
      { score: 7, weight: 1 },
      { score: 6, weight: 1 },
      { score: 7, weight: 1 }
    ]),
    6.5
  );
  assert.equal(
    partBandRaw([
      { score: 6, weight: 1 },
      { score: null, weight: 1 }
    ]),
    null
  );
  assert.equal(partBandRaw([]), null);
});

test('scoreCap (BR-17): mục "Giới hạn điểm" đang tick → maxBand nhỏ nhất kèm lý do', () => {
  assert.deepEqual(scoreCap(9, [], 250), { max: 9, reason: null });
  assert.deepEqual(
    scoreCap(
      9,
      [
        { content: 'Không chia đoạn', maxBand: 5 },
        { content: 'Lạc đề', maxBand: 4 }
      ],
      250
    ),
    { max: 4, reason: 'Lạc đề' }
  );
});

test('scoreCap (BR-18): bài ≤ 20 từ → tối đa 1', () => {
  assert.deepEqual(scoreCap(9, [{ content: 'Không chia đoạn', maxBand: 5 }], 18), {
    max: 1,
    reason: 'Bài chỉ có 18 từ (≤ 20 từ)'
  });
});

test('scoreOptions (BR-11): bước 1 → 10 mức; bước 0.5 → 19 mức', () => {
  assert.deepEqual(scoreOptions(9, 1), [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]);
  assert.equal(scoreOptions(9, 0.5).length, 19);
  assert.equal(scoreOptions(9, 0.5)[13], 6.5);
});

test('clampScore: vượt giới hạn mới → hạ xuống mức tối đa; chưa chấm giữ nguyên', () => {
  assert.equal(clampScore(7, 5), 5);
  assert.equal(clampScore(4, 5), 4);
  assert.equal(clampScore(null, 5), null);
});

test('paperScore / formatScore (BR-31): thang 10 làm tròn 2 chữ số, không làm tròn kiểu IELTS', () => {
  assert.equal(paperScore(7.666666), 7.67);
  assert.equal(paperScore(6.125), 6.13);
  assert.equal(paperScore(null), null);
  assert.equal(formatScore(8.75), '8.75');
  assert.equal(formatScore(7.5), '7.5');
  assert.equal(formatScore(9), '9');
});

test('memberScore (BR-31): điểm cả bài + cộng / trừ, kẹp trong [0, 10]', () => {
  assert.equal(memberScore(7.666666, 1), 8.67);
  assert.equal(memberScore(9.5, 2), 10);
  assert.equal(memberScore(1, -2), 0);
  assert.equal(memberScore(null, 1), null);
});

test('adjustmentOptions: -2 … +2 bước 0,25, không có 0', () => {
  const opts = adjustmentOptions();
  assert.equal(opts.length, 16);
  assert.equal(opts[0], -2);
  assert.equal(opts[15], 2);
  assert.ok(!opts.includes(0));
});

test('levelOfScore / scoreForLevel: mức chứa điểm; bấm mức giữ điểm nếu đã trong khoảng, nếu không lấy điểm cao nhất', () => {
  const levels = [
    { id: 1, minScore: 9, maxScore: 10 },
    { id: 2, minScore: 7, maxScore: 8.75 },
    { id: 3, minScore: 0, maxScore: 4.75 }
  ];
  assert.equal(levelOfScore(levels, 8)?.id, 2);
  assert.equal(levelOfScore(levels, 6), null);
  assert.equal(levelOfScore(levels, null), null);
  assert.equal(scoreForLevel(levels[1], 7.5), 7.5);
  assert.equal(scoreForLevel(levels[1], 9.5), 8.75);
  assert.equal(scoreForLevel(levels[0], null), 10);
});
