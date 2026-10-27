// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import type { AssignmentSummary } from '@/lib/types/assignment';

import { sortByDue, summarizeOpenAssignments } from './dashboard.ts';

const NOW = Date.parse('2026-11-05T10:00:00Z');
const HOUR = 3_600_000;

const asg = (id: number, patch: Partial<AssignmentSummary>): AssignmentSummary => ({
  id,
  title: `Bài ${id}`,
  status: 'PUBLISHED',
  openAt: null,
  dueAt: null,
  partCount: 2,
  publishedAt: null,
  createdAt: '2026-11-01T00:00:00Z',
  updatedAt: '2026-11-01T00:00:00Z',
  recipientCount: 10,
  completedCount: 4,
  pendingGradingCount: 3,
  ...patch
});

test('summarizeOpenAssignments: chỉ tính bài đang mở; sắp hết hạn = còn ≤ 48 giờ và chưa quá hạn', () => {
  const s = summarizeOpenAssignments(
    [
      asg(1, { dueAt: new Date(NOW + 10 * HOUR).toISOString() }),
      asg(2, { dueAt: new Date(NOW + 72 * HOUR).toISOString() }),
      asg(3, { dueAt: new Date(NOW - HOUR).toISOString() }),
      asg(4, { status: 'CLOSED', dueAt: new Date(NOW + HOUR).toISOString() })
    ],
    NOW
  );
  assert.deepEqual(s, { open: 3, dueSoon: 1, completed: 12, recipients: 30, pendingGrading: 9 });
});

test('sortByDue: hạn gần trước, không có hạn xếp cuối', () => {
  const sorted = sortByDue([
    asg(1, {}),
    asg(2, { dueAt: '2026-11-09T00:00:00Z' }),
    asg(3, { dueAt: '2026-11-06T00:00:00Z' })
  ]);
  assert.deepEqual(
    sorted.map((a) => a.id),
    [3, 2, 1]
  );
});
