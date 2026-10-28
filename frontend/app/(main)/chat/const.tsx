import type { ChannelType } from '@/lib/types/chat';

// ─── Hằng số màn Trao đổi ─────────────────────────────────────────────────────

export const CHANNEL_GROUPS: { type: ChannelType; label: string }[] = [
  { type: 'DIRECT', label: 'Trao đổi riêng' },
  { type: 'SUBMISSION', label: 'Theo bài nộp' },
  { type: 'CLASSROOM', label: 'Theo lớp' }
];
