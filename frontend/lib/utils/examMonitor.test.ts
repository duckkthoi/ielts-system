// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import type { ExamPush, MonitorRow } from '@/lib/types/exam';

import { applyExamPush, mergeFeed, monitorSummary, prependFeed } from './examMonitor.ts';

const row = (over: Partial<MonitorRow> = {}): MonitorRow => ({
  sessionId: 1,
  partId: 10,
  partTitle: 'Task 2',
  studentId: 100,
  studentCode: 'SV01',
  studentName: 'An',
  startedAt: '2026-11-05T10:00:00Z',
  endsAt: '2026-11-05T10:40:00Z',
  finishedAt: null,
  status: 'IN_PROGRESS',
  online: true,
  tabHiddenCount: 0,
  pasteCount: 0,
  disconnectCount: 0,
  lastEventAt: null,
  ...over
});

const push = (over: Partial<ExamPush> = {}): ExamPush => ({
  sessionId: 1,
  partId: 10,
  studentId: 100,
  studentName: 'An',
  type: 'TAB_HIDDEN',
  pasteChars: null,
  status: 'IN_PROGRESS',
  at: '2026-11-05T10:05:00Z',
  reportedLate: false,
  ...over
});

test('applyExamPush: rời tab / dán / mất kết nối tăng đúng bộ đếm, ghi lần gần nhất', () => {
  let rows = [row()];
  rows = applyExamPush(rows, push()).rows;
  rows = applyExamPush(rows, push({ type: 'PASTE', pasteChars: 300, at: '2026-11-05T10:06:00Z' })).rows;
  rows = applyExamPush(rows, push({ type: 'DISCONNECT', at: '2026-11-05T10:07:00Z' })).rows;
  assert.equal(rows[0].tabHiddenCount, 1);
  assert.equal(rows[0].pasteCount, 1);
  assert.equal(rows[0].disconnectCount, 1);
  assert.equal(rows[0].online, false);
  assert.equal(rows[0].lastEventAt, '2026-11-05T10:07:00Z');
  assert.equal(applyExamPush(rows, push({ type: 'RECONNECT' })).rows[0].online, true);
});

test('applyExamPush: rời cửa sổ (Alt+Tab) cũng tính là rời trang', () => {
  assert.equal(applyExamPush([row()], push({ type: 'WINDOW_BLUR' })).rows[0].tabHiddenCount, 1);
  assert.equal(applyExamPush([row()], push({ type: 'WINDOW_FOCUS' })).rows[0].tabHiddenCount, 0);
});

test('mergeFeed: gộp REST + tin đẩy, bỏ trùng, mới xảy ra trước (sự kiện gửi bù xếp đúng giờ)', () => {
  const rest = [push({ at: '2026-11-05T10:05:00Z' }), push({ type: 'STARTED', at: '2026-11-05T10:00:00Z' })];
  const live = [
    push({ at: '2026-11-05T10:05:00Z' }),
    push({ type: 'PASTE', at: '2026-11-05T10:03:00Z', reportedLate: true })
  ];
  assert.deepEqual(
    mergeFeed(rest, live).map((x) => x.type),
    ['TAB_HIDDEN', 'PASTE', 'STARTED']
  );
});

test('applyExamPush: quá giờ / nộp đổi trạng thái theo server, nộp ghi finishedAt', () => {
  const over = applyExamPush([row()], push({ type: 'OVERTIME', status: 'OVERTIME' })).rows;
  assert.equal(over[0].status, 'OVERTIME');
  const done = applyExamPush(
    over,
    push({ type: 'SUBMITTED', status: 'FINISHED_OVERTIME', at: '2026-11-05T10:45:00Z' })
  ).rows;
  assert.equal(done[0].status, 'FINISHED_OVERTIME');
  assert.equal(done[0].finishedAt, '2026-11-05T10:45:00Z');
});

test('applyExamPush: phiên chưa có (vừa bắt đầu) → unknown để tải lại, không đổi danh sách', () => {
  const rows = [row()];
  const res = applyExamPush(rows, push({ sessionId: 2, type: 'STARTED' }));
  assert.equal(res.unknown, true);
  assert.equal(res.rows, rows);
});

test('prependFeed: mới nhất lên đầu, bỏ trùng, giữ tối đa limit', () => {
  let feed: ExamPush[] = [];
  feed = prependFeed(feed, push({ at: 'a' }), 2);
  feed = prependFeed(feed, push({ at: 'b' }), 2);
  feed = prependFeed(feed, push({ at: 'b' }), 2);
  feed = prependFeed(feed, push({ at: 'c' }), 2);
  assert.deepEqual(
    feed.map((f) => f.at),
    ['c', 'b']
  );
});

test('monitorSummary: đếm đang thi / quá giờ / đã nộp / có cảnh báo', () => {
  const s = monitorSummary([
    row({ sessionId: 1, tabHiddenCount: 2 }),
    row({ sessionId: 2, status: 'OVERTIME' }),
    row({ sessionId: 3, status: 'FINISHED_OVERTIME', pasteCount: 1 }),
    row({ sessionId: 4, status: 'FINISHED' })
  ]);
  assert.deepEqual(s, { inProgress: 1, overtime: 1, finished: 2, flagged: 2 });
});
