// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { computeClockOffset, serverNow, syncServerClock } from './serverClock.ts';

const T = Date.parse('2026-11-05T10:00:00Z');

test('computeClockOffset: máy chậm 90 giây → độ lệch +90 000', () => {
  assert.equal(computeClockOffset('2026-11-05T10:00:00Z', T - 90_000), 90_000);
});

test('computeClockOffset: biết giờ gửi và nhận → lấy điểm giữa', () => {
  // Gửi lúc T − 1 s, nhận lúc T + 1 s, server trả đúng T → máy không lệch.
  assert.equal(computeClockOffset('2026-11-05T10:00:00Z', T - 1_000, T + 1_000), 0);
});

test('computeClockOffset: chuỗi micro giây của server vẫn đọc được', () => {
  assert.equal(computeClockOffset('2026-11-05T10:00:00.250123Z', T), 250);
});

test('computeClockOffset: chuỗi hỏng → 0, không làm lệch đồng hồ', () => {
  assert.equal(computeClockOffset('không phải ngày', T), 0);
});

test('syncServerClock + serverNow: cộng độ lệch vào giờ máy; null thì giữ nguyên', () => {
  syncServerClock(new Date(Date.now() + 3_600_000).toISOString());
  assert.ok(Math.abs(serverNow() - Date.now() - 3_600_000) < 1_000);
  syncServerClock(null);
  assert.ok(Math.abs(serverNow() - Date.now() - 3_600_000) < 1_000);
  syncServerClock(new Date().toISOString());
});
