// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import type { ProgressCellDto, ProgressMatrix } from '@/lib/types/stats';

import { applyPresenceEvent, applyProgressEvent, remindTargets, submittedPerPart } from './progress.ts';

const cell = (partId: number, patch: Partial<ProgressCellDto> = {}): ProgressCellDto => ({
  partId,
  submissionId: null,
  status: 'NOT_STARTED',
  currentVersion: 0,
  late: false,
  overdue: false,
  score: null,
  ...patch
});

const matrix = (): ProgressMatrix => ({
  parts: [
    { id: 11, orderNo: 1, title: 'Task 1', taskType: 'WRITING_TASK1' },
    { id: 12, orderNo: 2, title: 'Task 2', taskType: 'WRITING_TASK2' }
  ],
  rows: [
    {
      student: { id: 1, studentCode: 'SV1', fullName: 'An' },
      online: false,
      viewingPartId: null,
      cells: [cell(11, { submissionId: 5, status: 'RETURNED', currentVersion: 1, score: 6 }), cell(12, { overdue: true })]
    },
    {
      student: { id: 2, studentCode: 'SV2', fullName: 'Bình' },
      online: true,
      viewingPartId: 12,
      cells: [cell(11, { submissionId: 7, status: 'SUBMITTED', currentVersion: 1 }), cell(12, { status: 'DRAFT', submissionId: 8 })]
    }
  ],
  summary: { students: 2, completedCount: 0, completedRate: 0, lateCount: 0, overdueCount: 1 }
});

const at = '2026-11-05T10:00:00Z';

test('applyProgressEvent: nộp → SUBMITTED, tăng version, ghi muộn, bỏ quá hạn, chưa có band', () => {
  const next = applyProgressEvent(matrix(), {
    submissionId: 9, studentId: 1, partId: 12, status: 'SUBMITTED', late: true, score: null, at
  });
  assert.deepEqual(next.rows[0].cells[1], cell(12, { submissionId: 9, status: 'SUBMITTED', currentVersion: 1, late: true }));
  assert.equal(next.rows[0].cells[0].score, 6, 'ô khác giữ nguyên');
});

test('applyProgressEvent: trả bài / yêu cầu sửa → lấy band của sự kiện', () => {
  const next = applyProgressEvent(matrix(), {
    submissionId: 7, studentId: 2, partId: 11, status: 'REVISION_REQUESTED', late: false, score: 5.5, at
  });
  assert.equal(next.rows[1].cells[0].status, 'REVISION_REQUESTED');
  assert.equal(next.rows[1].cells[0].score, 5.5);
  assert.equal(next.rows[1].cells[0].currentVersion, 1);
});

test('applyProgressEvent: nộp lại bản sửa → band cũ bị bỏ (lần mới chưa trả)', () => {
  const next = applyProgressEvent(matrix(), {
    submissionId: 5, studentId: 1, partId: 11, status: 'SUBMITTED', late: false, score: null, at
  });
  assert.equal(next.rows[0].cells[0].currentVersion, 2);
  assert.equal(next.rows[0].cells[0].score, null);
});

test('applyProgressEvent: sinh viên không có trong ma trận → giữ nguyên tham chiếu', () => {
  const m = matrix();
  assert.equal(applyProgressEvent(m, { submissionId: 1, studentId: 99, partId: 11, status: 'DRAFT', late: false, score: null, at }), m);
});

test('applyPresenceEvent: online / đang mở phần; offline thì bỏ phần đang mở', () => {
  const on = applyPresenceEvent(matrix(), { studentId: 1, online: true, viewingPartId: 11, at });
  assert.equal(on.rows[0].online, true);
  assert.equal(on.rows[0].viewingPartId, 11);
  const off = applyPresenceEvent(matrix(), { studentId: 2, online: false, viewingPartId: 12, at });
  assert.equal(off.rows[1].online, false);
  assert.equal(off.rows[1].viewingPartId, null);
});

test('remindTargets: ưu tiên hàng đang chọn; không chọn → người còn phần phải nộp', () => {
  assert.deepEqual(remindTargets(matrix(), [2]), [2]);
  assert.deepEqual(remindTargets(matrix(), []), [1, 2]);
  assert.deepEqual(remindTargets(matrix(), [], 11), [], 'Task 1 ai cũng đã nộp');
  assert.deepEqual(remindTargets(matrix(), [], 12), [1, 2]);
});

test('submittedPerPart: đếm ô đã có ít nhất một lần nộp', () => {
  assert.deepEqual(submittedPerPart(matrix()), { 11: 2, 12: 0 });
});
