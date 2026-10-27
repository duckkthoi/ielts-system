import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type { ChannelSummary, ChatHistory } from '@/lib/types/chat';

export interface StudentOption {
  id: number;
  studentCode: string;
  fullName: string;
}

// ─── Trao đổi (docs/05 mục A.9) ───────────────────────────────────────────────
// Chỉ đọc qua REST; gửi tin, "đang gõ", "đã đọc" đi qua STOMP (`/app/chat/{id}/…`).

/** Mọi lớp mình dạy / học và các bài nộp đã có tin; kênh có tin mới nhất trước. */
export async function channels(): Promise<ChannelSummary[]> {
  const res = (await axiosClient.get('/api/chat/channels', { controlLoading: true })) as unknown as ApiResponse<
    ChannelSummary[]
  >;
  return res.data;
}

/** Tải ngược về trước: `beforeId` = id tin cũ nhất đang có. Không thuộc kênh → 404 (trang tự báo, tắt toast). */
export async function messages(channelId: string, beforeId?: number, size?: number): Promise<ChatHistory> {
  const res = (await axiosClient.get(formatUrl('/api/chat/{channelId}/messages', { channelId }), {
    params: { beforeId, size },
    controlLoading: true,
    toastError: false
  })) as unknown as ApiResponse<ChatHistory>;
  return res.data;
}

export async function searchStudents(keyword: string): Promise<StudentOption[]> {
  const res = (await axiosClient.get('/api/teacher/students/search', {
    params: { keyword },
    controlLoading: true
  })) as unknown as ApiResponse<StudentOption[]>;
  return res.data;
}

export async function openDirect(studentId: number): Promise<ChannelSummary> {
  const res = (await axiosClient.post(`/api/chat/direct/${studentId}`)) as unknown as ApiResponse<ChannelSummary>;
  return res.data;
}

export const chatApi = { channels, messages, searchStudents, openDirect };
