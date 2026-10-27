import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type {
  AssignmentDetail,
  AssignmentListParams,
  AssignmentRequest,
  AssignmentSettingsRequest,
  AssignmentSummary,
  BandDescriptors,
  RubricTemplateDto,
  StudentOption,
  Targets,
  TargetsRequest,
  TaskType
} from '@/lib/types/assignment';

// ─── Bài tập của giáo viên (docs/05 mục A.5) ──────────────────────────────────
// Thao tác có message thành công từ server thì trả cả ApiResponse để trang hiện đúng câu đó.

const URL = {
  list: '/api/teacher/assignments',
  one: '/api/teacher/assignments/{id}',
  settings: '/api/teacher/assignments/{id}/settings',
  publish: '/api/teacher/assignments/{id}/publish',
  targets: '/api/teacher/assignments/{id}/targets',
  close: '/api/teacher/assignments/{id}/close',
  reopen: '/api/teacher/assignments/{id}/reopen',
  duplicate: '/api/teacher/assignments/{id}/duplicate'
} as const;

export async function list(params: AssignmentListParams): Promise<ApiResponse<AssignmentSummary[]>> {
  return (await axiosClient.get(URL.list, { params, controlLoading: true })) as unknown as ApiResponse<
    AssignmentSummary[]
  >;
}

export async function detail(id: number): Promise<AssignmentDetail> {
  const res = (await axiosClient.get(formatUrl(URL.one, { id }), {
    controlLoading: true
  })) as unknown as ApiResponse<AssignmentDetail>;
  return res.data;
}

/** Trình soạn tự hiện lỗi validate (message chỉ rõ phần / tiêu chí) nên tắt toast. */
export async function create(body: AssignmentRequest): Promise<AssignmentDetail> {
  const res = (await axiosClient.post(URL.list, body, {
    toastError: false
  })) as unknown as ApiResponse<AssignmentDetail>;
  return res.data;
}

export async function replaceDraft(id: number, body: AssignmentRequest): Promise<AssignmentDetail> {
  const res = (await axiosClient.put(formatUrl(URL.one, { id }), body, {
    toastError: false
  })) as unknown as ApiResponse<AssignmentDetail>;
  return res.data;
}

export async function updateSettings(id: number, body: AssignmentSettingsRequest): Promise<AssignmentDetail> {
  const res = (await axiosClient.patch(formatUrl(URL.settings, { id }), body, {
    toastError: false
  })) as unknown as ApiResponse<AssignmentDetail>;
  return res.data;
}

export async function remove(id: number): Promise<ApiResponse<null>> {
  return (await axiosClient.delete(formatUrl(URL.one, { id }))) as unknown as ApiResponse<null>;
}

/** Thiếu điều kiện → 400 NOT_READY, message liệt kê mọi vấn đề: hộp thoại tự hiện nên tắt toast. */
export async function publish(id: number, body: TargetsRequest): Promise<ApiResponse<{ recipientCount: number }>> {
  return (await axiosClient.post(formatUrl(URL.publish, { id }), body, {
    toastError: false
  })) as unknown as ApiResponse<{ recipientCount: number }>;
}

export async function getTargets(id: number): Promise<Targets> {
  const res = (await axiosClient.get(formatUrl(URL.targets, { id }), {
    controlLoading: true
  })) as unknown as ApiResponse<Targets>;
  return res.data;
}

export async function addTargets(
  id: number,
  body: TargetsRequest
): Promise<ApiResponse<{ newRecipients: number; recipientCount: number }>> {
  return (await axiosClient.put(formatUrl(URL.targets, { id }), body, {
    toastError: false
  })) as unknown as ApiResponse<{ newRecipients: number; recipientCount: number }>;
}

export async function close(id: number): Promise<ApiResponse<AssignmentDetail>> {
  return (await axiosClient.post(formatUrl(URL.close, { id }))) as unknown as ApiResponse<AssignmentDetail>;
}

export async function reopen(id: number): Promise<ApiResponse<AssignmentDetail>> {
  return (await axiosClient.post(formatUrl(URL.reopen, { id }))) as unknown as ApiResponse<AssignmentDetail>;
}

export async function duplicate(id: number): Promise<ApiResponse<AssignmentDetail>> {
  return (await axiosClient.post(formatUrl(URL.duplicate, { id }))) as unknown as ApiResponse<AssignmentDetail>;
}

export async function rubricTemplates(taskType?: TaskType): Promise<RubricTemplateDto[]> {
  const res = (await axiosClient.get('/api/teacher/rubric-templates', {
    params: { taskType },
    controlLoading: true
  })) as unknown as ApiResponse<RubricTemplateDto[]>;
  return res.data;
}

/** Bảng mô tả band thu gọn (diễn giải tiếng Việt, docs/08 mục 5.4) + link bản chính thức. Dữ liệu tĩnh. */
export async function bandDescriptors(taskType?: TaskType): Promise<BandDescriptors> {
  const res = (await axiosClient.get('/api/teacher/rubric-templates/descriptors', {
    params: { taskType },
    controlLoading: true,
    toastError: false
  })) as unknown as ApiResponse<BandDescriptors>;
  return res.data;
}

/** Tìm sinh viên theo mã / tên để giao bài cho từng người (tối đa 20 kết quả). */
export async function searchStudents(keyword: string): Promise<StudentOption[]> {
  const res = (await axiosClient.get('/api/teacher/students/search', {
    params: { keyword },
    controlLoading: true
  })) as unknown as ApiResponse<StudentOption[]>;
  return res.data;
}

export const assignmentApi = {
  list,
  detail,
  create,
  replaceDraft,
  updateSettings,
  remove,
  publish,
  getTargets,
  bandDescriptors,
  addTargets,
  close,
  reopen,
  duplicate,
  rubricTemplates,
  searchStudents
};
