import type { AxiosProgressEvent, AxiosResponse } from 'axios';

import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type {
  AddMembersResult,
  ClassroomFormValues,
  ClassroomSummary,
  ImportReport,
  ListParams,
  MemberDto
} from '@/lib/types/classroom';
import { filenameFromDisposition, saveBlob } from '@/lib/utils/download';

// ─── Lớp học + import sinh viên (docs/05 mục A.4) ─────────────────────────────

export async function listClasses(params: ListParams): Promise<ApiResponse<ClassroomSummary[]>> {
  return (await axiosClient.get('/api/teacher/classes', {
    params,
    controlLoading: true
  })) as unknown as ApiResponse<ClassroomSummary[]>;
}

export async function getClass(id: number): Promise<ClassroomSummary> {
  const res = (await axiosClient.get(formatUrl('/api/teacher/classes/{id}', { id }), {
    controlLoading: true
  })) as unknown as ApiResponse<ClassroomSummary>;
  return res.data;
}

/** Form tự hiện lỗi theo trường (mã lớp trùng, sai định dạng) nên tắt toast. */
export async function createClass(data: ClassroomFormValues): Promise<ClassroomSummary> {
  const res = (await axiosClient.post('/api/teacher/classes', data, {
    toastError: false
  })) as unknown as ApiResponse<ClassroomSummary>;
  return res.data;
}

export async function updateClass(id: number, data: ClassroomFormValues): Promise<ClassroomSummary> {
  const res = (await axiosClient.put(formatUrl('/api/teacher/classes/{id}', { id }), data, {
    toastError: false
  })) as unknown as ApiResponse<ClassroomSummary>;
  return res.data;
}

export async function deleteClass(id: number): Promise<void> {
  await axiosClient.delete(formatUrl('/api/teacher/classes/{id}', { id }));
}

export async function listMembers(id: number, params: ListParams): Promise<ApiResponse<MemberDto[]>> {
  return (await axiosClient.get(formatUrl('/api/teacher/classes/{id}/members', { id }), {
    params,
    controlLoading: true
  })) as unknown as ApiResponse<MemberDto[]>;
}

export async function addMembers(id: number, studentCodes: string[]): Promise<AddMembersResult> {
  const res = (await axiosClient.post(formatUrl('/api/teacher/classes/{id}/members', { id }), {
    studentCodes
  })) as unknown as ApiResponse<AddMembersResult>;
  return res.data;
}

export async function removeMember(id: number, studentId: number): Promise<void> {
  await axiosClient.delete(formatUrl('/api/teacher/classes/{id}/members/{studentId}', { id, studentId }));
}

/**
 * Import file danh sách sinh viên (multipart). Hộp thoại import tự hiện tiến độ tải lên và lỗi cả file
 * (`BAD_FILE`) nên không bật spinner toàn cục, không toast.
 */
export async function importStudents(
  id: number,
  file: File,
  sendEmail: boolean,
  onUploadProgress?: (event: AxiosProgressEvent) => void
): Promise<ImportReport> {
  const form = new FormData();
  form.append('file', file);
  form.append('sendEmail', String(sendEmail));
  const res = (await axiosClient.post(formatUrl('/api/teacher/classes/{id}/import', { id }), form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    controlLoading: true,
    toastError: false,
    timeout: 120_000,
    onUploadProgress
  })) as unknown as ApiResponse<ImportReport>;
  return res.data;
}

async function download(url: string, fallbackName: string, params?: Record<string, string>) {
  const res = (await axiosClient.get(url, { params, responseType: 'blob', rawResponse: true })) as AxiosResponse<Blob>;
  const header = res.headers['content-disposition'] as string | undefined;
  saveBlob(res.data, filenameFromDisposition(header) ?? fallbackName);
}

/** File tài khoản vừa tạo: chỉ tải được một lần, hết hạn sau 15 phút. */
export async function downloadCredentials(token: string): Promise<void> {
  await download(formatUrl('/api/teacher/imports/{token}/credentials', { token }), 'tai-khoan-sinh-vien.xlsx');
}

export async function downloadTemplate(format: 'xlsx' | 'csv'): Promise<void> {
  await download('/api/teacher/import-template', `mau-danh-sach-sinh-vien.${format}`, { format });
}

export const classroomApi = {
  listClasses,
  getClass,
  createClass,
  updateClass,
  deleteClass,
  listMembers,
  addMembers,
  removeMember,
  importStudents,
  downloadCredentials,
  downloadTemplate
};
