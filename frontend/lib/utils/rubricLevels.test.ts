// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { defaultLevels, levelProblems } from './rubricLevels.ts';

test('defaultLevels: thang 10 bước 0,25 → 9–10, 7–8,75, 5–6,75, 0–4,75; bước 1 → 9–10, 7–8, 5–6, 0–4', () => {
  assert.deepEqual(
    defaultLevels(10, 0.25).map((l) => [l.label, l.minScore, l.maxScore]),
    [
      ['Xuất sắc', 9, 10],
      ['Tốt', 7, 8.75],
      ['Đạt', 5, 6.75],
      ['Chưa đạt', 0, 4.75]
    ]
  );
  assert.deepEqual(
    defaultLevels(10, 1).map((l) => [l.minScore, l.maxScore]),
    [
      [9, 10],
      [7, 8],
      [5, 6],
      [0, 4]
    ]
  );
  assert.deepEqual(levelProblems(defaultLevels(10, 0.25), 10, 0.25), []);
});

test('levelProblems (BR-31): rỗng hợp lệ; 1 mức, trùng tên, vượt thang, sai bước, chồng khoảng đều báo', () => {
  assert.deepEqual(levelProblems([], 10, 0.25), []);
  assert.deepEqual(levelProblems([{ label: 'A', minScore: 0, maxScore: 10 }], 10, 0.25), ['Cần ít nhất 2 mức đạt']);
  assert.deepEqual(
    levelProblems(
      [
        { label: 'Tốt', minScore: 5, maxScore: 8 },
        { label: 'tốt', minScore: 9, maxScore: 10 }
      ],
      10,
      0.25
    ),
    ['Mức "tốt" bị lặp']
  );
  assert.deepEqual(
    levelProblems(
      [
        { label: 'A', minScore: 0, maxScore: 4.1 },
        { label: 'B', minScore: 5, maxScore: 10.25 }
      ],
      10,
      0.25
    ),
    ['Mức "A": điểm phải là bội số của 0.25', 'Mức "B": cần 0 ≤ thấp nhất ≤ cao nhất ≤ 10']
  );
  assert.deepEqual(
    levelProblems(
      [
        { label: 'A', minScore: 5, maxScore: 8 },
        { label: 'B', minScore: 7, maxScore: 10 }
      ],
      10,
      0.25
    ),
    ['Mức "A" và "B" chồng khoảng điểm']
  );
});
