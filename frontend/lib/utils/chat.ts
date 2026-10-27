// ─── Trao đổi (chat): gộp tin, cập nhật danh sách kênh, "đã xem" ───────────────
// Hàm thuần (có test). Tin mình gửi hiện ngay ở trạng thái "đang gửi" (khoá theo `clientMsgId`),
// server phát lại tin đã lưu (kể cả khi gửi trùng) thì thay bằng bản của server.

import type { ChannelSummary, ChatMessageEvent, ChatReader } from '@/lib/types/chat';

export const MAX_MESSAGE_LENGTH = 4000;

export type LocalStatus = 'sending' | 'failed';

/** Một dòng trong khung chat: tin của server (`id`) hoặc tin mình vừa gửi chưa được xác nhận. */
export interface ChatLine {
  key: string;
  id: number | null;
  senderId: number;
  senderName: string;
  content: string;
  clientMsgId: string | null;
  createdAt: string;
  local: LocalStatus | null;
}

export const lineFromEvent = (m: ChatMessageEvent): ChatLine => ({
  key: `m-${m.id}`,
  id: m.id,
  senderId: m.senderId,
  senderName: m.senderName,
  content: m.content,
  clientMsgId: m.clientMsgId,
  createdAt: m.createdAt,
  local: null
});

/**
 * Gộp tin của server vào danh sách đang hiện: bỏ trùng theo `id`, thay tin "đang gửi" cùng `clientMsgId`
 * của chính mình. Tin của server sắp theo `id` tăng dần; tin chưa xác nhận luôn ở cuối.
 */
export function mergeMessages(lines: ChatLine[], incoming: ChatMessageEvent[], myId: number): ChatLine[] {
  const confirmed = new Map<number, ChatLine>();
  const pending: ChatLine[] = [];
  for (const line of lines) {
    if (line.id !== null) confirmed.set(line.id, line);
    else pending.push(line);
  }
  const acked = new Set<string>();
  for (const m of incoming) {
    confirmed.set(m.id, lineFromEvent(m));
    if (m.senderId === myId && m.clientMsgId) acked.add(m.clientMsgId);
  }
  return [
    ...[...confirmed.values()].sort((a, b) => (a.id as number) - (b.id as number)),
    ...pending.filter((p) => !p.clientMsgId || !acked.has(p.clientMsgId))
  ];
}

/** Đổi trạng thái một tin chưa xác nhận (gửi lại / gửi lỗi). */
export function setLocalStatus(lines: ChatLine[], clientMsgId: string, status: LocalStatus): ChatLine[] {
  return lines.map((l) => (l.id === null && l.clientMsgId === clientMsgId ? { ...l, local: status } : l));
}

/** Nội dung gửi được: 1 – 4000 ký tự sau khi bỏ khoảng trắng hai đầu (như server). */
export function normalizeMessage(text: string): string | null {
  const content = text.trim();
  return content.length === 0 || content.length > MAX_MESSAGE_LENGTH ? null : content;
}

/**
 * Có tin mới trên một kênh: cập nhật tin cuối, tăng số chưa đọc nếu là tin của người khác và kênh không
 * đang mở, đưa kênh lên đầu. Kênh chưa có trong danh sách → trả `null` để trang tải lại danh sách.
 */
export function applyIncomingToChannels(
  channels: ChannelSummary[],
  m: ChatMessageEvent,
  myId: number,
  activeChannelId: string | null
): ChannelSummary[] | null {
  const current = channels.find((c) => c.channelId === m.channelId);
  if (!current) return null;
  if (current.lastMessage && current.lastMessage.id >= m.id) return channels;
  const updated: ChannelSummary = {
    ...current,
    lastMessage: { id: m.id, senderName: m.senderName, preview: m.content.slice(0, 120), createdAt: m.createdAt },
    unreadCount: m.senderId !== myId && m.channelId !== activeChannelId ? current.unreadCount + 1 : current.unreadCount
  };
  return [updated, ...channels.filter((c) => c.channelId !== m.channelId)];
}

/** Ghi nhận một mốc "đã đọc" (chỉ tăng, như server). */
export function upsertReader(reads: ChatReader[], reader: ChatReader): ChatReader[] {
  const old = reads.find((r) => r.userId === reader.userId);
  if (old && old.lastMessageId >= reader.lastMessageId) return reads;
  return [
    ...reads.filter((r) => r.userId !== reader.userId),
    { ...reader, userName: reader.userName || old?.userName || '' }
  ];
}

/** Tên những người (khác mình) đã đọc tới tin `messageId`. */
export function seenBy(reads: ChatReader[], messageId: number, myId: number): string[] {
  return reads.filter((r) => r.userId !== myId && r.lastMessageId >= messageId).map((r) => r.userName);
}
