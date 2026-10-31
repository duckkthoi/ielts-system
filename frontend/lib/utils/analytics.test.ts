// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { delta, formatRatio, heatGrid, heatLevel } from './analytics.ts';

test('formatRatio: ghi rõ tử / mẫu; mẫu số 0 thì không chia', () => {
  assert.equal(formatRatio({ numerator: 31, denominator: 50, rate: 0.62 }), '62% (31/50)');
  assert.equal(formatRatio({ numerator: 0, denominator: 0, rate: null }), '—');
});

test('delta: thiếu kỳ trước → null', () => {
  assert.equal(delta(6.5, 6.25), 0.25);
  assert.equal(delta(5, null), null);
});

test('heatGrid + heatLevel: đủ 7 × 24 ô, mức theo giá trị lớn nhất', () => {
  const grid = heatGrid([
    { dow: 7, hour: 23, count: 4 },
    { dow: 9, hour: 1, count: 3 }
  ]);
  assert.equal(grid.length, 7);
  assert.equal(grid[6][23], 4);
  assert.equal(
    grid.flat().reduce((a, b) => a + b, 0),
    4
  );
  assert.equal(heatLevel(0, 4), 0);
  assert.equal(heatLevel(1, 4), 1);
  assert.equal(heatLevel(4, 4), 4);
});
