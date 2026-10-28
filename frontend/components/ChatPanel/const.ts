import type { ChannelType } from '@/lib/types/chat';

// ─── Hằng số khung chat (dùng ở màn Trao đổi và màn làm bài) ───────────────────

/** Số tin mỗi lần tải lịch sử (server mặc định 50, tối đa 100). */
export const PAGE_SIZE = 50;
/** Gửi "đang gõ" tối đa 1 lần / 3 giây (docs/05 B.2). */
export const TYPING_THROTTLE_MS = 3_000;
/** Không nhận thêm "đang gõ" sau chừng này thì ẩn dòng "… đang gõ". */
export const TYPING_VISIBLE_MS = 4_000;
/** Server chưa phát lại tin sau chừng này → coi như gửi lỗi, cho gửi lại. */
export const SEND_TIMEOUT_MS = 10_000;

/** `submission-12` → { type: 'SUBMISSION', refId: 12 }; sai dạng → null. */
export function parseChannelId(channelId: string): { type: ChannelType; refId: number } | null {
  const m = /^(submission|class|direct)-(\d+)$/.exec(channelId);
  if (!m) return null;
  return {
    type: m[1] === 'submission' ? 'SUBMISSION' : m[1] === 'class' ? 'CLASSROOM' : 'DIRECT',
    refId: Number(m[2])
  };
}
