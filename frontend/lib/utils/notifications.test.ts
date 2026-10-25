// Chạy: npm run test:unit
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { groupByDay, latestCreatedAt, mergeNotifications } from './notifications.ts';

const n = (id: number, createdAt: string, title = `#${id}`) => ({ id, createdAt, title });

test('mergeNotifications: bỏ trùng theo id, bản mới thắng, mới nhất trước', () => {
  const current = [n(2, '2026-11-08T03:00:00Z'), n(1, '2026-11-08T01:00:00Z')];
  const incoming = [n(3, '2026-11-08T04:00:00Z'), n(2, '2026-11-08T03:00:00Z', 'đã đọc')];
  const merged = mergeNotifications(current, incoming);
  assert.deepEqual(
    merged.map((x) => x.id),
    [3, 2, 1]
  );
  assert.equal(merged[1].title, 'đã đọc');
});

test('mergeNotifications: cắt theo limit, cùng thời điểm thì id lớn trước', () => {
  const merged = mergeNotifications(
    [n(1, '2026-11-08T01:00:00Z')],
    [n(5, '2026-11-08T01:00:00Z'), n(4, '2026-11-07T01:00:00Z')],
    2
  );
  assert.deepEqual(
    merged.map((x) => x.id),
    [5, 1]
  );
});

test('latestCreatedAt: lấy thời điểm mới nhất, danh sách rỗng → null', () => {
  assert.equal(
    latestCreatedAt([n(1, '2026-11-08T01:00:00Z'), n(2, '2026-11-08T05:00:00Z'), n(3, '2026-11-07T09:00:00Z')]),
    '2026-11-08T05:00:00Z'
  );
  assert.equal(latestCreatedAt([]), null);
});

test('groupByDay: Hôm nay / Hôm qua / Trước đó theo giờ máy, bỏ nhóm rỗng', () => {
  const now = new Date(2026, 10, 9, 10, 0); // 09/11/2026 10:00 giờ máy
  const items = [
    n(1, new Date(2026, 10, 9, 0, 5).toISOString()),
    n(2, new Date(2026, 10, 8, 23, 59).toISOString()),
    n(3, new Date(2026, 10, 7, 12, 0).toISOString())
  ];
  assert.deepEqual(
    groupByDay(items, now).map((g) => [g.group, g.items.map((x) => x.id)]),
    [
      ['Hôm nay', [1]],
      ['Hôm qua', [2]],
      ['Trước đó', [3]]
    ]
  );
  assert.deepEqual(
    groupByDay([items[2]], now).map((g) => g.group),
    ['Trước đó']
  );
});
