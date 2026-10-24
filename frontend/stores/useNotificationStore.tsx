'use client';

import { create } from 'zustand';

import { notificationApi } from '@/lib/api/notification';
import type { NotificationDto } from '@/lib/types/notification';
import { latestCreatedAt, mergeNotifications } from '@/lib/utils/notifications';

// ─── Thông báo mới nhất + số chưa đọc (chuông trên header) ────────────────────
// Trang /thong-bao tự tải danh sách đầy đủ; store chỉ giữ vài thông báo gần nhất để hiện trong chuông
// và làm mốc `after` khi lấy bù sau kết nối lại (SD-09).

const RECENT_LIMIT = 20;
/** Lấy bù tối đa chừng này thông báo bị lỡ trong một lần (đủ cho một lần mất mạng). */
const SYNC_SIZE = 50;

interface NotificationState {
  items: NotificationDto[];
  unreadCount: number;
  /** Lô thông báo vừa đến (WebSocket, hoặc lấy bù sau kết nối lại) — trang /thong-bao chèn lên đầu. */
  incoming: NotificationDto[];
  load: () => Promise<void>;
  /** Nhận một thông báo mới (WebSocket). Trả false nếu đã có rồi (trùng). */
  receive: (notification: NotificationDto) => boolean;
  /** Lấy các thông báo tạo sau thông báo mới nhất đang có. Trả về các thông báo vừa lấy được. */
  sync: () => Promise<NotificationDto[]>;
  markRead: (id: number) => Promise<void>;
  markAllRead: () => Promise<void>;
  reset: () => void;
}

export const useNotificationStore = create<NotificationState>()((set, get) => ({
  items: [],
  unreadCount: 0,
  incoming: [],

  load: async () => {
    const [page, unreadCount] = await Promise.all([
      notificationApi.list({ page: 0, size: RECENT_LIMIT }),
      notificationApi.unreadCount()
    ]);
    set({ items: page.data, unreadCount });
  },

  receive: (notification) => {
    const { items, unreadCount } = get();
    if (items.some((n) => n.id === notification.id)) return false;
    set({
      items: mergeNotifications(items, [notification], RECENT_LIMIT),
      unreadCount: notification.readAt ? unreadCount : unreadCount + 1,
      incoming: [notification]
    });
    return true;
  },

  sync: async () => {
    const after = latestCreatedAt(get().items);
    if (!after) {
      await get().load();
      return [];
    }
    const [page, unreadCount] = await Promise.all([
      notificationApi.list({ after, page: 0, size: SYNC_SIZE }),
      notificationApi.unreadCount()
    ]);
    const known = new Set(get().items.map((n) => n.id));
    const missed = page.data.filter((n) => !known.has(n.id));
    set({ items: mergeNotifications(get().items, missed, RECENT_LIMIT), unreadCount });
    if (missed.length > 0) set({ incoming: missed });
    return missed;
  },

  markRead: async (id) => {
    const target = get().items.find((n) => n.id === id);
    if (target?.readAt) return;
    const updated = await notificationApi.markRead(id);
    set((state) => ({
      items: state.items.map((n) => (n.id === id ? updated : n)),
      unreadCount: Math.max(0, state.unreadCount - 1)
    }));
  },

  markAllRead: async () => {
    await notificationApi.markAllRead();
    const now = new Date().toISOString();
    set((state) => ({ items: state.items.map((n) => (n.readAt ? n : { ...n, readAt: now })), unreadCount: 0 }));
  },

  reset: () => set({ items: [], unreadCount: 0, incoming: [] })
}));
