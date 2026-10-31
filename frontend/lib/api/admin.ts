import type { AxiosProgressEvent, AxiosResponse } from 'axios';

import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type {
  AuditLog,
  AuditLogParams,
  CreatedTeacher,
  SessionListParams,
  SessionRow,
  SystemStats,
  TeacherFormValues,
  TeacherImportReport,
  UserDetail,
  UserListParams,
  UserSummary
} from '@/lib/types/admin';
import type { ApiResponse } from '@/lib/types/api';
import { filenameFromDisposition, saveBlob } from '@/lib/utils/download';

// ─── Quản trị (docs/05 mục A.3) ───────────────────────────────────────────────
// Danh sách trả cả ApiResponse để lấy `totalRecord`; còn lại trả `data` đã bóc.

export async function listTeachers(params: UserListParams): Promise<ApiResponse<UserSummary[]>> {
  return (await axiosClient.get('/api/admin/teachers', { params })) as unknown as ApiResponse<UserSummary[]>;
}

/** Trang tự hiện lỗi theo từng trường (`fieldErrors`) nên tắt toast. */
export async function createTeacher(data: TeacherFormValues): Promise<CreatedTeacher> {
  const res = (await axiosClient.post('/api/admin/teachers', data, {
    toastError: false
  })) as unknown as ApiResponse<CreatedTeacher>;
  return res.data;
}

export async function updateTeacher(id: number, data: Omit<TeacherFormValues, 'username'>): Promise<UserSummary> {
  const res = (await axiosClient.put(formatUrl('/api/admin/teachers/{id}', { id }), data, {
    toastError: false
  })) as unknown as ApiResponse<UserSummary>;
  return res.data;
}

/** Lỗi cả file (BAD_FILE) do hộp thoại tự hiện nên tắt toast. */
export async function importTeachers(
  file: File,
  onUploadProgress?: (event: AxiosProgressEvent) => void
): Promise<TeacherImportReport> {
  const form = new FormData();
  form.append('file', file);
  const res = (await axiosClient.post('/api/admin/teachers/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    toastError: false,
    onUploadProgress
  })) as unknown as ApiResponse<TeacherImportReport>;
  return res.data;
}

export async function downloadTeacherTemplate(format: 'xlsx' | 'csv'): Promise<void> {
  const res = (await axiosClient.get('/api/admin/teachers/import-template', {
    params: { format },
    responseType: 'blob',
    rawResponse: true
  })) as AxiosResponse<Blob>;
  const header = res.headers['content-disposition'] as string | undefined;
  saveBlob(res.data, filenameFromDisposition(header) ?? `mau-danh-sach-giao-vien.${format}`);
}

export async function listUsers(params: UserListParams): Promise<ApiResponse<UserSummary[]>> {
  return (await axiosClient.get('/api/admin/users', { params })) as unknown as ApiResponse<UserSummary[]>;
}

export async function getUser(id: number): Promise<UserDetail> {
  const res = (await axiosClient.get(formatUrl('/api/admin/users/{id}', { id }))) as unknown as ApiResponse<UserDetail>;
  return res.data;
}

export async function lockUser(id: number): Promise<ApiResponse<UserSummary>> {
  return (await axiosClient.post(
    formatUrl('/api/admin/users/{id}/lock', { id })
  )) as unknown as ApiResponse<UserSummary>;
}

export async function unlockUser(id: number): Promise<ApiResponse<UserSummary>> {
  return (await axiosClient.post(
    formatUrl('/api/admin/users/{id}/unlock', { id })
  )) as unknown as ApiResponse<UserSummary>;
}

export async function resetPassword(id: number): Promise<string> {
  const res = (await axiosClient.post(
    formatUrl('/api/admin/users/{id}/reset-password', { id })
  )) as unknown as ApiResponse<{
    temporaryPassword: string;
  }>;
  return res.data.temporaryPassword;
}

export async function getStats(): Promise<SystemStats> {
  const res = (await axiosClient.get('/api/admin/stats', {
    controlLoading: true
  })) as unknown as ApiResponse<SystemStats>;
  return res.data;
}

export async function listAuditLogs(params: AuditLogParams): Promise<ApiResponse<AuditLog[]>> {
  return (await axiosClient.get('/api/admin/audit-logs', { params })) as unknown as ApiResponse<AuditLog[]>;
}

export async function listSessions(params: SessionListParams): Promise<ApiResponse<SessionRow[]>> {
  return (await axiosClient.get('/api/admin/sessions', {
    params,
    controlLoading: true
  })) as unknown as ApiResponse<SessionRow[]>;
}

export async function revokeSession(id: number): Promise<ApiResponse<SessionRow>> {
  return (await axiosClient.delete(
    formatUrl('/api/admin/sessions/{id}', { id })
  )) as unknown as ApiResponse<SessionRow>;
}

export async function revokeUserSessions(userId: number): Promise<ApiResponse<{ count: number }>> {
  return (await axiosClient.post(
    formatUrl('/api/admin/users/{id}/sessions/revoke', { id: userId })
  )) as unknown as ApiResponse<{
    count: number;
  }>;
}

export async function cleanupSessions(): Promise<ApiResponse<{ count: number }>> {
  return (await axiosClient.post('/api/admin/sessions/cleanup')) as unknown as ApiResponse<{ count: number }>;
}

export const adminApi = {
  listTeachers,
  createTeacher,
  importTeachers,
  downloadTeacherTemplate,
  updateTeacher,
  listUsers,
  getUser,
  lockUser,
  unlockUser,
  resetPassword,
  getStats,
  listAuditLogs,
  listSessions,
  revokeSession,
  revokeUserSessions,
  cleanupSessions
};
