// ─── Trao đổi (chat) theo bài nộp / theo lớp (docs/05 mục A.9, B.2; UC-29, SD-12) ─
// `channelId` dạng `submission-{id}`, `class-{id}` hoặc `direct-{id}`.

export type ChannelType = 'SUBMISSION' | 'CLASSROOM' | 'DIRECT';

export interface ChannelSummary {
  channelId: string;
  type: ChannelType;
  /** Lớp, bài nộp hoặc cuộc trao đổi riêng với một học sinh. */
  title: string;
  lastMessage: { id: number; senderName: string; preview: string; createdAt: string } | null;
  /** Tin của người khác sau mốc "đã đọc" của mình. */
  unreadCount: number;
}

export interface ChatMessageEvent {
  type: 'MESSAGE';
  id: number;
  channelId: string;
  senderId: number;
  senderName: string;
  content: string;
  clientMsgId: string | null;
  createdAt: string;
}

export interface ChatTypingEvent {
  type: 'TYPING';
  userId: number;
  userName: string;
}

export interface ChatReadEvent {
  type: 'READ';
  userId: number;
  lastMessageId: number;
}

export type ChatEvent = ChatMessageEvent | ChatTypingEvent | ChatReadEvent;

export interface ChatReader {
  userId: number;
  userName: string;
  lastMessageId: number;
}

/** `messages` mới nhất trước. */
export interface ChatHistory {
  messages: ChatMessageEvent[];
  reads: ChatReader[];
}
