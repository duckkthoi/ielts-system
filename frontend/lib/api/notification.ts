import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type { NotificationDto, NotificationListParams } from '@/lib/types/notification';

// ─── Thông báo (docs/05 mục A.9) ──────────────────────────────────────────────
// Gọi nền (chuông, đồng bộ sau kết nối lại) nên không bật spinner toàn cục.

/** Trả cả `totalRecord` để trang biết còn trang sau không. */
export async function list(params: NotificationListParams): Promise<ApiResponse<NotificationDto[]>> {
  return (await axiosClient.get('/api/notifications', { params, controlLoading: true })) as unknown as ApiResponse<
    NotificationDto[]
  >;
}

export async function unreadCount(): Promise<number> {
  const res = (await axiosClient.get('/api/notifications/unread-count', {
    controlLoading: true
  })) as unknown as ApiResponse<{ count: number }>;
  return res.data.count;
}

export async function markRead(id: number): Promise<NotificationDto> {
  const res = (await axiosClient.post(formatUrl('/api/notifications/{id}/read', { id }), null, {
    controlLoading: true
  })) as unknown as ApiResponse<NotificationDto>;
  return res.data;
}

export async function markAllRead(): Promise<number> {
  const res = (await axiosClient.post('/api/notifications/read-all', null, {
    controlLoading: true
  })) as unknown as ApiResponse<{ updated: number }>;
  return res.data.updated;
}

export const notificationApi = { list, unreadCount, markRead, markAllRead };
