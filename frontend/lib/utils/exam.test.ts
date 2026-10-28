// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { examStatusAt, formatExamClock, nextAway } from './exam.ts';

const ENDS = '2026-11-05T10:40:00Z';
const T = Date.parse(ENDS);

test('examStatusAt: chưa nộp — còn giờ / tới đúng giờ kết thúc là quá giờ (khớp ExamRules)', () => {
  assert.equal(examStatusAt(ENDS, null, T - 1_000), 'IN_PROGRESS');
  assert.equal(examStatusAt(ENDS, null, T), 'OVERTIME');
});

test('examStatusAt: nộp đúng giờ kết thúc vẫn trong giờ, sau đó là nộp quá giờ', () => {
  assert.equal(examStatusAt(ENDS, '2026-11-05T10:40:00Z', T + 60_000), 'FINISHED');
  assert.equal(examStatusAt(ENDS, '2026-11-05T10:40:01Z', T + 60_000), 'FINISHED_OVERTIME');
});

test('formatExamClock: còn giờ làm tròn lên, dưới 5 phút là gấp', () => {
  assert.deepEqual(formatExamClock(ENDS, T - 40 * 60_000), { text: '40:00', overtime: false, urgent: false });
  assert.deepEqual(formatExamClock(ENDS, T - 4 * 60_000 - 500), { text: '04:01', overtime: false, urgent: true });
  assert.deepEqual(formatExamClock(ENDS, T - 2 * 3_600_000 - 3_000), {
    text: '2:00:03',
    overtime: false,
    urgent: false
  });
});

test('formatExamClock: quá giờ hiện "+" và thời gian đã quá', () => {
  assert.deepEqual(formatExamClock(ENDS, T), { text: '+00:00', overtime: true, urgent: false });
  assert.deepEqual(formatExamClock(ENDS, T + 190_000), { text: '+03:10', overtime: true, urgent: false });
});

test('nextAway: chuyển tab (mất focus rồi ẩn) chỉ ra một TAB_HIDDEN, quay lại ra một TAB_VISIBLE', () => {
  assert.deepEqual(nextAway(null, true, false), { away: 'TAB', event: 'TAB_HIDDEN' });
  assert.deepEqual(nextAway('TAB', true, false), { away: 'TAB', event: null });
  assert.deepEqual(nextAway('TAB', false, true), { away: null, event: 'TAB_VISIBLE' });
});

test('nextAway: Alt+Tab sang ứng dụng khác (tab vẫn hiện) → WINDOW_BLUR, quay lại → WINDOW_FOCUS', () => {
  assert.deepEqual(nextAway(null, false, false), { away: 'WINDOW', event: 'WINDOW_BLUR' });
  assert.deepEqual(nextAway('WINDOW', false, false), { away: 'WINDOW', event: null });
  assert.deepEqual(nextAway('WINDOW', false, true), { away: null, event: 'WINDOW_FOCUS' });
});

test('nextAway: đang rời cửa sổ rồi thu nhỏ → thêm TAB_HIDDEN; tab hiện lại mà chưa focus → TAB_VISIBLE, còn rời cửa sổ', () => {
  assert.deepEqual(nextAway('WINDOW', true, false), { away: 'TAB', event: 'TAB_HIDDEN' });
  assert.deepEqual(nextAway('TAB', false, false), { away: 'WINDOW', event: 'TAB_VISIBLE' });
  assert.deepEqual(nextAway(null, false, true), { away: null, event: null });
});
