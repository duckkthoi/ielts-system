import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type { GradeDto, GradingScreen, QueueParams, QueueRow, SaveGradeRequest } from '@/lib/types/grading';

// ─── Giáo viên chấm bài (docs/05 mục A.7) ─────────────────────────────────────

const URL = {
  queue: '/api/teacher/submissions',
  screen: '/api/teacher/submissions/{id}',
  grade: '/api/teacher/submissions/{id}/grade',
  return: '/api/teacher/submissions/{id}/return',
  revision: '/api/teacher/submissions/{id}/request-revision'
} as const;

/** Trả cả `totalRecord` để phân trang. Luôn sắp theo thời điểm nộp. */
export async function queue(params: QueueParams): Promise<ApiResponse<QueueRow[]>> {
  return (await axiosClient.get(URL.queue, { params, controlLoading: true })) as unknown as ApiResponse<QueueRow[]>;
}

/** Bài nộp của giáo viên khác → 404 (BR-15): trang tự hiện "Không tìm thấy" nên tắt toast. */
export async function screen(id: number, versionNo?: number): Promise<GradingScreen> {
  const res = (await axiosClient.get(formatUrl(URL.screen, { id }), {
    params: { versionNo },
    controlLoading: true,
    toastError: false
  })) as unknown as ApiResponse<GradingScreen>;
  return res.data;
}

/** Tự lưu chạy nền: lỗi hiện cạnh nút "Lưu nháp" nên tắt toast. */
export async function saveGrade(id: number, body: SaveGradeRequest): Promise<GradeDto> {
  const res = (await axiosClient.put(formatUrl(URL.grade, { id }), body, {
    controlLoading: true,
    toastError: false
  })) as unknown as ApiResponse<GradeDto>;
  return res.data;
}

export async function returnGrade(id: number): Promise<GradingScreen> {
  const res = (await axiosClient.post(formatUrl(URL.return, { id }))) as unknown as ApiResponse<GradingScreen>;
  return res.data;
}

export async function requestRevision(
  id: number,
  body: { note: string | null; dueAt: string }
): Promise<GradingScreen> {
  const res = (await axiosClient.post(formatUrl(URL.revision, { id }), body)) as unknown as ApiResponse<GradingScreen>;
  return res.data;
}

export const gradingApi = { queue, screen, saveGrade, returnGrade, requestRevision };
