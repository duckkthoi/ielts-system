import type { AxiosResponse } from 'axios';

import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type { AssignmentStats, ClassStats, ProgressMatrix, StudentStats } from '@/lib/types/stats';
import type { SubmissionStatus } from '@/lib/theme/status';
import { filenameFromDisposition, saveBlob } from '@/lib/utils/download';

// ─── Tiến độ, thống kê, kết quả học tập (docs/05 mục A.6, A.8) ────────────────
// Trang tự hiện Skeleton nên không bật spinner toàn cục.

const URL = {
  studentResults: '/api/student/results',
  progress: '/api/teacher/assignments/{id}/progress',
  remind: '/api/teacher/assignments/{id}/remind',
  assignmentStats: '/api/teacher/assignments/{id}/stats',
  exportGrades: '/api/teacher/assignments/{id}/export',
  classStats: '/api/teacher/stats/classes/{id}',
  studentStats: '/api/teacher/stats/students/{id}'
} as const;

async function get<T>(url: string, params?: object): Promise<T> {
  const res = (await axiosClient.get(url, { params, controlLoading: true })) as unknown as ApiResponse<T>;
  return res.data;
}

/** Sinh viên xem kết quả học tập của mình. */
export async function studentResults(): Promise<StudentStats> {
  return get<StudentStats>(URL.studentResults);
}

export async function progress(
  id: number,
  params: { classId?: number; status?: SubmissionStatus } = {}
): Promise<ProgressMatrix> {
  return get<ProgressMatrix>(formatUrl(URL.progress, { id }), params);
}

/** Gửi nhắc nhở thủ công; quá 3 lần / 10 phút → 429 RATE_LIMITED (server toast message). */
export async function remind(id: number, studentIds: number[], partId?: number): Promise<{ sent: number }> {
  const res = (await axiosClient.post(formatUrl(URL.remind, { id }), {
    studentIds,
    partId
  })) as unknown as ApiResponse<{ sent: number }>;
  return res.data;
}

export async function assignmentStats(id: number, classId?: number): Promise<AssignmentStats> {
  return get<AssignmentStats>(formatUrl(URL.assignmentStats, { id }), { classId });
}

/** Bảng điểm `.xlsx` (FR-66). */
export async function exportGrades(id: number, classId?: number): Promise<void> {
  const res = (await axiosClient.get(formatUrl(URL.exportGrades, { id }), {
    params: { classId },
    responseType: 'blob',
    rawResponse: true
  })) as AxiosResponse<Blob>;
  const header = res.headers['content-disposition'] as string | undefined;
  saveBlob(res.data, filenameFromDisposition(header) ?? `bang-diem-bai-${id}.xlsx`);
}

export async function classStats(id: number): Promise<ClassStats> {
  return get<ClassStats>(formatUrl(URL.classStats, { id }));
}

/** Giáo viên xem kết quả học tập của một sinh viên trên các bài của mình. */
export async function studentStats(id: number): Promise<StudentStats> {
  return get<StudentStats>(formatUrl(URL.studentStats, { id }));
}

export const statsApi = {
  studentResults,
  progress,
  remind,
  assignmentStats,
  exportGrades,
  classStats,
  studentStats
};
