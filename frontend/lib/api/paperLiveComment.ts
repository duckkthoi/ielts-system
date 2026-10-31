import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type { LiveComment, LiveCommentInput, LiveCommentKind, TeacherPaperDocument } from '@/lib/types/paper';

// ─── Nhận xét trực tiếp trên Paper đang viết (docs/05 mục A.10, H4) ───────────
// Danh sách ban đầu qua REST, thay đổi sau đó nghe ở `/topic/paper/{id}/comments`. Tải nền nên không bật spinner.

const URL = {
  list: '/api/papers/docs/{documentId}/comments',
  resolve: '/api/papers/docs/{documentId}/comments/{id}/resolve',
  create: '/api/teacher/papers/docs/{documentId}/comments',
  item: '/api/teacher/papers/docs/{documentId}/comments/{id}',
  documents: '/api/teacher/assignments/{id}/groups/documents'
} as const;

export async function list(documentId: number): Promise<LiveComment[]> {
  const res = (await axiosClient.get(formatUrl(URL.list, { documentId }), {
    controlLoading: true,
    toastError: false
  })) as unknown as ApiResponse<LiveComment[]>;
  return res.data;
}

export async function create(documentId: number, body: LiveCommentInput): Promise<ApiResponse<LiveComment>> {
  return (await axiosClient.post(formatUrl(URL.create, { documentId }), body, {
    controlLoading: true
  })) as unknown as ApiResponse<LiveComment>;
}

export async function update(
  documentId: number,
  id: number,
  body: { comment: string; kind: LiveCommentKind; suggestion?: string | null }
): Promise<ApiResponse<LiveComment>> {
  return (await axiosClient.put(formatUrl(URL.item, { documentId, id }), body, {
    controlLoading: true
  })) as unknown as ApiResponse<LiveComment>;
}

export async function remove(documentId: number, id: number): Promise<ApiResponse<null>> {
  return (await axiosClient.delete(formatUrl(URL.item, { documentId, id }), {
    controlLoading: true
  })) as unknown as ApiResponse<null>;
}

export async function resolve(documentId: number, id: number, resolved: boolean): Promise<ApiResponse<LiveComment>> {
  return (await axiosClient.put(
    formatUrl(URL.resolve, { documentId, id }),
    { resolved },
    { controlLoading: true }
  )) as unknown as ApiResponse<LiveComment>;
}

/** Giáo viên: các tài liệu nhóm đã mở của một bài, sửa gần nhất trước. */
export async function teacherDocuments(assignmentId: number): Promise<TeacherPaperDocument[]> {
  const res = (await axiosClient.get(formatUrl(URL.documents, { id: assignmentId }), {
    controlLoading: true
  })) as unknown as ApiResponse<TeacherPaperDocument[]>;
  return res.data;
}

export const paperLiveCommentApi = { list, create, update, remove, resolve, teacherDocuments };
