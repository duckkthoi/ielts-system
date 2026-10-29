import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type {
  AssignmentResult,
  DraftResponse,
  SaveDraftResponse,
  StudentAssignmentDetail,
  StudentAssignmentList,
  StudentTab,
  SubmitResponse,
  VersionDetail,
  VersionSummary
} from '@/lib/types/student';
import { syncServerClock } from '@/lib/utils/serverClock';

// ─── Sinh viên: bài được giao, lịch sử nộp, kết quả (docs/05 mục A.8) ─────────
// Trang tự hiện Skeleton nên không bật spinner toàn cục. Response có `serverTime` thì
// đồng bộ đồng hồ để CountdownTimer đếm theo giờ server (SD-11).

const URL = {
  assignments: '/api/student/assignments',
  assignment: '/api/student/assignments/{id}',
  result: '/api/student/assignments/{id}/result',
  versions: '/api/student/parts/{partId}/versions',
  version: '/api/student/parts/{partId}/versions/{versionNo}',
  draft: '/api/student/parts/{partId}/draft',
  submit: '/api/student/parts/{partId}/submit'
} as const;

export async function assignments(tab?: StudentTab): Promise<StudentAssignmentList> {
  const res = (await axiosClient.get(URL.assignments, {
    params: { tab },
    controlLoading: true
  })) as unknown as ApiResponse<StudentAssignmentList>;
  syncServerClock(res.data.serverTime);
  return res.data;
}

/** Không được giao / chưa tới giờ mở → 404: trang tự hiện "Không tìm thấy" nên tắt toast. */
export async function assignment(id: number): Promise<StudentAssignmentDetail> {
  const res = (await axiosClient.get(formatUrl(URL.assignment, { id }), {
    controlLoading: true,
    toastError: false
  })) as unknown as ApiResponse<StudentAssignmentDetail>;
  syncServerClock(res.data.serverTime);
  return res.data;
}

export async function result(id: number): Promise<AssignmentResult> {
  const res = (await axiosClient.get(formatUrl(URL.result, { id }), {
    controlLoading: true
  })) as unknown as ApiResponse<AssignmentResult>;
  return res.data;
}

/** Mới nhất trước. */
export async function versions(partId: number): Promise<VersionSummary[]> {
  const res = (await axiosClient.get(formatUrl(URL.versions, { partId }), {
    controlLoading: true
  })) as unknown as ApiResponse<VersionSummary[]>;
  return res.data;
}

export async function version(partId: number, versionNo: number): Promise<VersionDetail> {
  const res = (await axiosClient.get(formatUrl(URL.version, { partId, versionNo }), {
    controlLoading: true
  })) as unknown as ApiResponse<VersionDetail>;
  return res.data;
}

export async function draft(partId: number): Promise<DraftResponse> {
  const res = (await axiosClient.get(formatUrl(URL.draft, { partId }), {
    controlLoading: true
  })) as unknown as ApiResponse<DraftResponse>;
  return res.data;
}

/** Tự lưu chạy nền: trạng thái hiện ở AutosaveIndicator, 409 DRAFT_CONFLICT mở hộp thoại → tắt toast. */
export async function saveDraft(partId: number, content: string, draftVersion: number): Promise<SaveDraftResponse> {
  const res = (await axiosClient.put(
    formatUrl(URL.draft, { partId }),
    { content, draftVersion },
    { controlLoading: true, toastError: false }
  )) as unknown as ApiResponse<SaveDraftResponse>;
  return res.data;
}

/**
 * `expectedVersion` = `currentVersion` vừa đọc: chống nộp trùng (409 VERSION_CONFLICT). Bài PAPER gửi `content = null`
 * — server nộp snapshot tài liệu nhóm (B25a); `toastError = false` để màn hình tự xử lý `SNAPSHOT_STALE`.
 */
export async function submit(
  partId: number,
  content: string | null,
  expectedVersion: number,
  toastError = true
): Promise<SubmitResponse> {
  const res = (await axiosClient.post(
    formatUrl(URL.submit, { partId }),
    { content, expectedVersion },
    { toastError }
  )) as unknown as ApiResponse<SubmitResponse>;
  return res.data;
}

export const studentApi = { assignments, assignment, result, versions, version, draft, saveDraft, submit };
