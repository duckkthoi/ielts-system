import axiosClient from '@/lib/axiosClient';
import type { ApiResponse } from '@/lib/types/api';

// ─── Upload ảnh đề bài (docs/05 A.5): PNG / JPG / WEBP ≤ 5 MB → {url: "/uploads/…"} ─

export const IMAGE_ACCEPT = 'image/png,image/jpeg,image/webp';
export const IMAGE_MAX_BYTES = 5 * 1024 * 1024;

/** Ô upload tự hiện trạng thái đang tải; lỗi (sai loại, quá lớn) vẫn toast message của server. */
export async function uploadImage(file: File): Promise<string> {
  const form = new FormData();
  form.append('file', file);
  const res = (await axiosClient.post('/api/files', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    controlLoading: true
  })) as unknown as ApiResponse<{ url: string }>;
  return res.data.url;
}

export const fileApi = { uploadImage };
