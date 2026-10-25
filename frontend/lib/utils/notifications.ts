// ─── Hàm thuần cho danh sách thông báo ────────────────────────────────────────
// Không import alias `@/…` để `node --test` chạy trực tiếp được (xem notifications.test.ts).

interface NotificationLike {
  id: number;
  createdAt: string;
}

/**
 * Gộp thông báo mới (WebSocket, lấy bù sau kết nối lại) vào danh sách đang có:
 * bỏ trùng theo `id` (bản mới thắng), sắp mới nhất trước, giữ tối đa `limit` phần tử.
 */
export function mergeNotifications<T extends NotificationLike>(current: T[], incoming: T[], limit = Infinity): T[] {
  const byId = new Map<number, T>();
  for (const n of current) byId.set(n.id, n);
  for (const n of incoming) byId.set(n.id, n);
  return [...byId.values()]
    .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt) || b.id - a.id)
    .slice(0, limit);
}

/** Thời điểm tạo mới nhất trong danh sách — dùng làm `after` khi lấy bù (SD-09). */
export function latestCreatedAt(items: NotificationLike[]): string | null {
  let latest: NotificationLike | null = null;
  for (const n of items) {
    if (!latest || Date.parse(n.createdAt) > Date.parse(latest.createdAt)) latest = n;
  }
  return latest?.createdAt ?? null;
}

export type DayGroup = 'Hôm nay' | 'Hôm qua' | 'Trước đó';

/** Chia theo ngày (giờ máy người dùng): Hôm nay / Hôm qua / Trước đó, giữ thứ tự trong mỗi nhóm. */
export function groupByDay<T extends NotificationLike>(
  items: T[],
  now: Date = new Date()
): { group: DayGroup; items: T[] }[] {
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const startOfYesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1).getTime();
  const groups: Record<DayGroup, T[]> = { 'Hôm nay': [], 'Hôm qua': [], 'Trước đó': [] };
  for (const n of items) {
    const t = Date.parse(n.createdAt);
    groups[t >= startOfToday ? 'Hôm nay' : t >= startOfYesterday ? 'Hôm qua' : 'Trước đó'].push(n);
  }
  return (Object.keys(groups) as DayGroup[])
    .filter((group) => groups[group].length > 0)
    .map((group) => ({ group, items: groups[group] }));
}
