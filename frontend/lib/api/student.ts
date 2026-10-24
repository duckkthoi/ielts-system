import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type {
  AssignmentResult,
  StudentAssignmentDetail,
  StudentAssignmentList,
  StudentTab,
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
  versions: '/api/student/parts/{partId}/versions'
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

export const studentApi = { assignments, assignment, result, versions };
