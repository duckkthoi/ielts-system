import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type { StudentGroups, TeacherGroups } from '@/lib/types/paper';

// ─── Nhóm viết paper (docs/05 mục A.10) ───────────────────────────────────────
// Mọi thao tác trả lại toàn bộ màn hình để vẽ lại; lỗi (đủ người, đã qua hạn chốt…) axiosClient đã toast.

const URL = {
  teacher: '/api/teacher/assignments/{id}/groups',
  teacherOne: '/api/teacher/assignments/{id}/groups/{groupId}',
  teacherAuto: '/api/teacher/assignments/{id}/groups/auto',
  student: '/api/student/assignments/{id}/groups',
  studentJoin: '/api/student/assignments/{id}/groups/{groupId}/join',
  studentMine: '/api/student/assignments/{id}/groups/mine'
} as const;

type Res<T> = ApiResponse<T>;

// ─── Giáo viên ───

export async function teacherGroups(id: number): Promise<TeacherGroups> {
  return (
    (await axiosClient.get(formatUrl(URL.teacher, { id }), { controlLoading: true })) as unknown as Res<TeacherGroups>
  ).data;
}

export async function createGroup(id: number, name: string, studentIds: number[]): Promise<TeacherGroups> {
  return (
    (await axiosClient.post(formatUrl(URL.teacher, { id }), { name, studentIds })) as unknown as Res<TeacherGroups>
  ).data;
}

/** PUT = đổi tên + thay toàn bộ thành viên (ai đang ở nhóm khác thì được chuyển sang). */
export async function updateGroup(
  id: number,
  groupId: number,
  name: string,
  studentIds: number[]
): Promise<TeacherGroups> {
  return (
    (await axiosClient.put(formatUrl(URL.teacherOne, { id, groupId }), {
      name,
      studentIds
    })) as unknown as Res<TeacherGroups>
  ).data;
}

export async function deleteGroup(id: number, groupId: number): Promise<TeacherGroups> {
  return ((await axiosClient.delete(formatUrl(URL.teacherOne, { id, groupId }))) as unknown as Res<TeacherGroups>).data;
}

/** Chia ngẫu nhiên người chưa có nhóm (trong một lớp nếu có `classId`). */
export async function autoGroups(id: number, classId?: number): Promise<TeacherGroups> {
  return ((await axiosClient.post(formatUrl(URL.teacherAuto, { id }), { classId })) as unknown as Res<TeacherGroups>)
    .data;
}

// ─── Sinh viên ───

export async function studentGroups(id: number): Promise<StudentGroups> {
  return (
    (await axiosClient.get(formatUrl(URL.student, { id }), { controlLoading: true })) as unknown as Res<StudentGroups>
  ).data;
}

export async function createMyGroup(id: number, name: string): Promise<StudentGroups> {
  return ((await axiosClient.post(formatUrl(URL.student, { id }), { name })) as unknown as Res<StudentGroups>).data;
}

export async function joinGroup(id: number, groupId: number): Promise<StudentGroups> {
  return ((await axiosClient.post(formatUrl(URL.studentJoin, { id, groupId }))) as unknown as Res<StudentGroups>).data;
}

export async function leaveGroup(id: number): Promise<StudentGroups> {
  return ((await axiosClient.delete(formatUrl(URL.studentMine, { id }))) as unknown as Res<StudentGroups>).data;
}

export const paperApi = {
  teacherGroups,
  createGroup,
  updateGroup,
  deleteGroup,
  autoGroups,
  studentGroups,
  createMyGroup,
  joinGroup,
  leaveGroup
};
