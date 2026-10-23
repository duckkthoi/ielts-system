import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type {
  AuditLog,
  AuditLogParams,
  CreatedTeacher,
  SystemStats,
  TeacherFormValues,
  UserDetail,
  UserListParams,
  UserSummary
} from '@/lib/types/admin';
import type { ApiResponse } from '@/lib/types/api';

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

export const adminApi = {
  listTeachers,
  createTeacher,
  updateTeacher,
  listUsers,
  getUser,
  lockUser,
  unlockUser,
  resetPassword,
  getStats,
  listAuditLogs
};
