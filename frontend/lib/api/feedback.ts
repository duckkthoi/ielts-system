import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type { CommentBankDto, CriterionGroup, ErrorCodeDto } from '@/lib/types/grading';

// ─── Mã lỗi và ngân hàng nhận xét của giáo viên (docs/05 mục A.7, BR-21, BR-22) ─
// Dùng nền trên màn chấm nên không bật spinner toàn cục.

export async function errorCodes(): Promise<ErrorCodeDto[]> {
  const res = (await axiosClient.get('/api/teacher/error-codes', {
    controlLoading: true
  })) as unknown as ApiResponse<ErrorCodeDto[]>;
  return res.data;
}

/** Của tôi (hay dùng lên đầu) rồi tới mẫu của hệ thống; lọc theo nhóm tiêu chí thì kèm cả mục dùng chung. */
export async function commentBank(criterion?: CriterionGroup | null, keyword?: string): Promise<CommentBankDto[]> {
  const res = (await axiosClient.get('/api/teacher/comment-bank', {
    params: { criterion: criterion ?? undefined, keyword: keyword || undefined },
    controlLoading: true
  })) as unknown as ApiResponse<CommentBankDto[]>;
  return res.data;
}

export async function addComment(body: { criterion: CriterionGroup | null; content: string }): Promise<CommentBankDto> {
  const res = (await axiosClient.post('/api/teacher/comment-bank', body, {
    controlLoading: true
  })) as unknown as ApiResponse<CommentBankDto>;
  return res.data;
}

export async function deleteComment(id: number): Promise<void> {
  await axiosClient.delete(formatUrl('/api/teacher/comment-bank/{id}', { id }), { controlLoading: true });
}

/** Ghi nhận một lần chèn để nhận xét hay dùng lên đầu (mẫu hệ thống không đếm). */
export async function useComment(id: number): Promise<void> {
  await axiosClient.post(formatUrl('/api/teacher/comment-bank/{id}/use', { id }), null, {
    controlLoading: true,
    toastError: false
  });
}

export const feedbackApi = { errorCodes, commentBank, addComment, deleteComment, useComment };
