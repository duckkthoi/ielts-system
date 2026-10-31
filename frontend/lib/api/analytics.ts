import type { AxiosResponse } from 'axios';

import axiosClient from '@/lib/axiosClient';
import type { AnalyticsFilter, AnalyticsOverview, AnalyticsScope, AnalyticsStudentRow } from '@/lib/types/analytics';
import type { ApiResponse } from '@/lib/types/api';
import { filenameFromDisposition, saveBlob } from '@/lib/utils/download';

// ─── Thống kê tổng hợp (docs/05 mục A.9b, H5) ─────────────────────────────────
// Cùng API cho admin (`/api/admin/analytics…`) và giáo viên (`/api/teacher/analytics…`). Trang tự hiện Skeleton.

const base = (scope: AnalyticsScope) => `/api/${scope}/analytics`;

export async function overview(scope: AnalyticsScope, filter: AnalyticsFilter): Promise<AnalyticsOverview> {
  const res = (await axiosClient.get(base(scope), {
    params: filter,
    controlLoading: true
  })) as unknown as ApiResponse<AnalyticsOverview>;
  return res.data;
}

export async function students(
  scope: AnalyticsScope,
  filter: AnalyticsFilter & { alertsOnly: boolean; page: number; size: number }
): Promise<ApiResponse<AnalyticsStudentRow[]>> {
  return (await axiosClient.get(`${base(scope)}/students`, {
    params: filter,
    controlLoading: true
  })) as unknown as ApiResponse<AnalyticsStudentRow[]>;
}

/** Bảng sinh viên theo đúng bộ lọc đang xem, `.csv` hoặc `.xlsx`. */
export async function exportStudents(
  scope: AnalyticsScope,
  filter: AnalyticsFilter,
  format: 'CSV' | 'XLSX'
): Promise<void> {
  const res = (await axiosClient.get(`${base(scope)}/export`, {
    params: { ...filter, format },
    responseType: 'blob',
    rawResponse: true
  })) as AxiosResponse<Blob>;
  const header = res.headers['content-disposition'] as string | undefined;
  saveBlob(res.data, filenameFromDisposition(header) ?? `thong-ke-sinh-vien.${format.toLowerCase()}`);
}

export const analyticsApi = { overview, students, exportStudents };
