// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { describeDue } from './due.ts';

const NOW = Date.parse('2026-11-05T10:00:00Z');
const at = (ms: number) => new Date(NOW + ms).toISOString();
const MIN = 60_000;
const HOUR = 60 * MIN;

test('describeDue: còn hơn 1 ngày → số ngày, tone thường', () => {
  assert.deepEqual(describeDue(at(2 * 24 * HOUR + 5 * HOUR), NOW), { text: 'Còn 2 ngày', tone: 'normal' });
});

test('describeDue: ≤ 24 giờ → hh:mm:ss; < 2 giờ warning; < 15 phút error', () => {
  assert.deepEqual(describeDue(at(5 * HOUR + 12 * MIN + 30_000), NOW), { text: 'Còn 05:12:30', tone: 'normal' });
  assert.deepEqual(describeDue(at(HOUR + 45 * MIN), NOW), { text: 'Còn 01:45:00', tone: 'warning' });
  assert.deepEqual(describeDue(at(12 * MIN + 5_000), NOW), { text: 'Còn 00:12:05', tone: 'error' });
});

test('describeDue: đã qua hạn → phút / giờ / ngày', () => {
  assert.deepEqual(describeDue(at(-20 * 1000), NOW), { text: 'Quá hạn 1 phút', tone: 'overdue' });
  assert.deepEqual(describeDue(at(-3 * HOUR - MIN), NOW), { text: 'Quá hạn 3 giờ', tone: 'overdue' });
  assert.deepEqual(describeDue(at(-50 * HOUR), NOW), { text: 'Quá hạn 2 ngày', tone: 'overdue' });
});
