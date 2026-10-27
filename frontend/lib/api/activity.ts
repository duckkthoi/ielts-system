import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type { StudentActivityEvent, StudentActivityType } from '@/lib/types/activity';

const URL = {
  studentActivity: '/api/student/assignments/{id}/activity',
  teacherActivity: '/api/teacher/assignments/{id}/activity'
} as const;

/** Ghi nhận trạng thái tab của sinh viên khi đang ở màn làm bài. */
export async function recordStudentActivity(id: number, type: StudentActivityType): Promise<StudentActivityEvent> {
  const res = (await axiosClient.post(
    formatUrl(URL.studentActivity, { id }),
    { type },
    { controlLoading: true, toastError: false }
  )) as unknown as ApiResponse<StudentActivityEvent>;
  return res.data;
}

/** Giáo viên tải lịch sử gần nhất của bài; tối đa 200 sự kiện. */
export async function teacherActivity(
  id: number,
  params: { classId?: number; limit?: number } = {}
): Promise<StudentActivityEvent[]> {
  const res = (await axiosClient.get(formatUrl(URL.teacherActivity, { id }), {
    params,
    controlLoading: true
  })) as unknown as ApiResponse<StudentActivityEvent[]>;
  return res.data;
}

export const activityApi = { recordStudentActivity, teacherActivity };
