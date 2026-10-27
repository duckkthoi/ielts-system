// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { clampScore, partBandRaw, roundBand, scoreCap, scoreOptions } from './grading.ts';

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
