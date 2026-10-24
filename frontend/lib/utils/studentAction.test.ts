// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { nextStudentAction } from './studentAction.ts';

const part = (partId: number, status: string, canSubmit = true) => ({ partId, status, canSubmit });

test('nextStudentAction: phần "Cần sửa" còn nộp được thắng phần chưa làm', () => {
  assert.deepEqual(nextStudentAction([part(1, 'NOT_STARTED'), part(2, 'REVISION_REQUESTED')]), {
    kind: 'revise',
    partId: 2
  });
});

test('nextStudentAction: chưa làm / đang làm → "Làm bài" phần đầu tiên còn nộp được', () => {
  assert.deepEqual(nextStudentAction([part(1, 'SUBMITTED'), part(2, 'DRAFT'), part(3, 'NOT_STARTED')]), {
    kind: 'write',
    partId: 2
  });
});

test('nextStudentAction: quá hạn không cho nộp muộn (canSubmit = false) → không hiện "Làm bài"', () => {
  assert.deepEqual(nextStudentAction([part(1, 'NOT_STARTED', false)]), { kind: 'view', partId: null });
  assert.deepEqual(nextStudentAction([part(1, 'REVISION_REQUESTED', false), part(2, 'RETURNED')]), {
    kind: 'result',
    partId: null
  });
});

test('nextStudentAction: đã nộp hết, có phần đã trả → "Xem kết quả"; chưa trả → "Xem chi tiết"', () => {
  assert.deepEqual(nextStudentAction([part(1, 'RETURNED'), part(2, 'SUBMITTED')]), { kind: 'result', partId: null });
  assert.deepEqual(nextStudentAction([part(1, 'SUBMITTED')]), { kind: 'view', partId: null });
});
