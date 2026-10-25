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

export interface ClockSample {
  serverTime: string;
  sentAt: number;
  receivedAt: number;
}

/** SD-11: trong các lần gọi GET /api/time, lấy lần có thời gian đi về nhỏ nhất (ít sai số nhất). */
export function bestClockOffset(samples: readonly ClockSample[]): number | null {
  const valid = samples.filter((s) => !Number.isNaN(Date.parse(s.serverTime)) && s.receivedAt >= s.sentAt);
  if (valid.length === 0) return null;
  const best = valid.reduce((a, b) => (b.receivedAt - b.sentAt < a.receivedAt - a.sentAt ? b : a));
  return computeClockOffset(best.serverTime, best.sentAt, best.receivedAt);
}

export function setClockOffset(ms: number) {
  offsetMs = ms;
}

/** "Bây giờ" theo giờ server (ms). */
export function serverNow(): number {
  return Date.now() + offsetMs;
}
