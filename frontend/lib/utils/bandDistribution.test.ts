// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { fillBandBins, overallOnTimeRate } from './bandDistribution.ts';

test('fillBandBins: thêm các mức 0.5 còn trống ở giữa, giữ thứ tự tăng dần', () => {
  assert.deepEqual(
    fillBandBins([
      { band: 5, count: 2 },
      { band: 6.5, count: 1 }
    ]),
    [
      { band: 5, count: 2 },
      { band: 5.5, count: 0 },
      { band: 6, count: 0 },
      { band: 6.5, count: 1 }
    ]
  );
});

test('fillBandBins: một mức / không có dữ liệu', () => {
  assert.deepEqual(fillBandBins([{ band: 0, count: 3 }]), [{ band: 0, count: 3 }]);
  assert.deepEqual(fillBandBins([]), []);
});

test('overallOnTimeRate: gộp mọi phần; chưa ai nộp → null', () => {
  assert.equal(
    overallOnTimeRate([
      { submittedCount: 10, lateCount: 1 },
      { submittedCount: 10, lateCount: 3 }
    ]),
    0.8
  );
  assert.equal(overallOnTimeRate([{ submittedCount: 0, lateCount: 0 }]), null);
});
