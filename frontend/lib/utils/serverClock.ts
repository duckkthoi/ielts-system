// ─── Giờ server phía client (SD-11) ───────────────────────────────────────────
// Giờ máy người dùng có thể lệch; đếm ngược phải theo giờ server. Lưu độ lệch (server − máy)
// mỗi khi có `serverTime` (response bài được giao, GET /api/time) rồi cộng vào Date.now().
// Hàm thuần, không import alias để `node --test` chạy trực tiếp.

let offsetMs = 0;

/**
 * Độ lệch server − máy. Biết thời điểm gửi và nhận thì lấy điểm giữa (bù nửa thời gian đi về);
 * không biết thời điểm gửi thì coi như nhận ngay.
 */
export function computeClockOffset(serverTime: string, sentAt: number, receivedAt: number = sentAt): number {
  const server = Date.parse(serverTime);
  if (Number.isNaN(server)) return 0;
  return Math.round(server - (sentAt + receivedAt) / 2);
}

export function syncServerClock(
  serverTime: string | null | undefined,
  sentAt: number = Date.now(),
  receivedAt?: number
) {
  if (serverTime) offsetMs = computeClockOffset(serverTime, sentAt, receivedAt);
}

/** "Bây giờ" theo giờ server (ms). */
export function serverNow(): number {
  return Date.now() + offsetMs;
}
