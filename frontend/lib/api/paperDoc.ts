import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type { DocumentState, UpdatesAfter } from '@/lib/types/paper';

// ─── Tài liệu paper cùng sửa qua REST (docs/05 mục A.10) ──────────────────────
// Gửi update đi qua STOMP (`/app/paper/{id}/update`), không qua đây. Chạy nền nên không bật spinner toàn cục.

const URL = {
  open: '/api/student/parts/{partId}/document',
  state: '/api/papers/docs/{id}',
  updates: '/api/papers/docs/{id}/updates',
  snapshot: '/api/papers/docs/{id}/snapshot'
} as const;

/** Mở tài liệu của nhóm tôi cho giai đoạn này (tạo nếu chưa có). Chưa có nhóm → 409 NO_GROUP: trang tự báo. */
export async function open(partId: number): Promise<DocumentState> {
  const res = (await axiosClient.get(formatUrl(URL.open, { partId }), {
    controlLoading: true,
    toastError: false
  })) as unknown as ApiResponse<DocumentState>;
  return res.data;
}

export async function state(id: number): Promise<DocumentState> {
  const res = (await axiosClient.get(formatUrl(URL.state, { id }), {
    controlLoading: true
  })) as unknown as ApiResponse<DocumentState>;
  return res.data;
}

export async function updatesAfter(id: number, afterSeq: number): Promise<UpdatesAfter> {
  const res = (await axiosClient.get(formatUrl(URL.updates, { id }), {
    params: { afterSeq },
    controlLoading: true
  })) as unknown as ApiResponse<UpdatesAfter>;
  return res.data;
}

/** Lưu snapshot đã gộp tới `upToSeq` kèm JSON + chữ thuần. Chạy nền: lỗi không toast (lần sau thử lại). */
export async function saveSnapshot(
  id: number,
  body: { state: string; upToSeq: number; contentJson: string; contentText: string }
): Promise<DocumentState> {
  const res = (await axiosClient.put(formatUrl(URL.snapshot, { id }), body, {
    controlLoading: true,
    toastError: false
  })) as unknown as ApiResponse<DocumentState>;
  return res.data;
}

export const paperDocApi = { open, state, updatesAfter, saveSnapshot };
