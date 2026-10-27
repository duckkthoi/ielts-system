// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import type { AssignmentBand, PartBand } from '@/lib/types/stats';

import { bandDomain, buildBandTrend, timeScale } from './bandTrend.ts';

const asg = (id: number, band: number | null, complete: boolean, at: string): AssignmentBand => ({
  assignmentId: id,
  title: `Bài ${id}`,
  partCount: 2,
  returnedParts: complete ? 2 : 1,
  complete,
  band,
  returnedAt: at
});

const part = (id: number, taskType: PartBand['taskType'], band: number | null, at: string): PartBand => ({
  assignmentId: 1,
  assignmentTitle: 'Tuần 1',
  partId: id,
  partTitle: `Phần ${id}`,
  taskType,
  band,
  returnedAt: at
});

test('buildBandTrend: band cả bài chỉ lấy bài đủ phần, sắp theo thời gian', () => {
  const series = buildBandTrend(
    [
      asg(2, 6.5, true, '2026-11-10T00:00:00Z'),
      asg(1, 6, true, '2026-11-01T00:00:00Z'),
      asg(3, null, false, '2026-11-12T00:00:00Z')
    ],
    []
  );
  assert.equal(series.length, 1);
  assert.equal(series[0].key, 'overall');
  assert.deepEqual(
    series[0].points.map((p) => p.band),
    [6, 6.5]
  );
});

test('buildBandTrend: tách Task 1 / Task 2, bỏ phần Tự do và chuỗi rỗng, giữ thứ tự cố định', () => {
  const series = buildBandTrend(
    [],
    [
      part(1, 'WRITING_TASK2', 5.5, '2026-11-02T00:00:00Z'),
      part(2, 'CUSTOM', 7, '2026-11-03T00:00:00Z'),
      part(3, 'WRITING_TASK1', 6, '2026-11-04T00:00:00Z'),
      part(4, 'WRITING_TASK1', null, '2026-11-05T00:00:00Z')
    ]
  );
  assert.deepEqual(
    series.map((s) => s.key),
    ['task1', 'task2']
  );
  assert.equal(series[0].points.length, 1);
  assert.equal(series[1].points[0].label, 'Tuần 1 · Phần 1');
});

test('bandDomain: nới 1 band, kẹp trong [0, 9], rộng ít nhất 2', () => {
  assert.deepEqual(bandDomain([]), [0, 9]);
  assert.deepEqual(bandDomain([5.5, 6.5]), [4, 8]);
  assert.deepEqual(bandDomain([8.5, 9]), [7, 9]);
  assert.deepEqual(bandDomain([0]), [0, 2]);
  assert.deepEqual(bandDomain([6]), [5, 7]);
});

test('timeScale: đưa về [0, 1]; một thời điểm → 0.5', () => {
  const scale = timeScale([100, 300, 200]);
  assert.equal(scale(100), 0);
  assert.equal(scale(200), 0.5);
  assert.equal(scale(300), 1);
  assert.equal(timeScale([42])(42), 0.5);
});
