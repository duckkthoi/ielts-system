// ─── Hằng số dùng toàn app ───────────────────────────────────────────────────

export const ROLE = {
  ADMIN: 'ADMIN',
  TEACHER: 'TEACHER',
  STUDENT: 'STUDENT'
} as const;

export type Role = (typeof ROLE)[keyof typeof ROLE];

export const ROLE_LABEL: Record<Role, string> = {
  ADMIN: 'Quản trị viên',
  TEACHER: 'Giáo viên',
  STUDENT: 'Sinh viên'
};

/** Trang chủ của từng vai trò sau khi đăng nhập. */
export const ROLE_HOME: Record<Role, string> = {
  ADMIN: '/quan-tri/tong-quan',
  TEACHER: '/giao-vien/tong-quan',
  STUDENT: '/sinh-vien/tong-quan'
};

/** Tiền tố đường dẫn mà từng vai trò được phép truy cập (ngoài các trang dùng chung). */
export const ROLE_PATH_PREFIX: Record<Role, string> = {
  ADMIN: '/quan-tri',
  TEACHER: '/giao-vien',
  STUDENT: '/sinh-vien'
};

/** Trang dùng chung cho mọi vai trò đã đăng nhập. */
export const SHARED_PATHS = ['/thong-bao', '/ca-nhan', '/chat'] as const;

export const ROUTES = {
  login: '/dang-nhap',
  changePassword: '/doi-mat-khau'
} as const;

/** Loại phần bài (TaskType của server). */
export const TASK_TYPE_LABEL: Record<'WRITING_TASK1' | 'WRITING_TASK2' | 'CUSTOM', string> = {
  WRITING_TASK1: 'IELTS Writing Task 1',
  WRITING_TASK2: 'IELTS Writing Task 2',
  CUSTOM: 'Tự do'
};

/** Tiêu chí IELTS theo mã (docs/08 mục 2). */
export const CRITERION_CODE_LABEL: Record<'TA' | 'TR' | 'CC' | 'LR' | 'GRA', string> = {
  TA: 'Task Achievement',
  TR: 'Task Response',
  CC: 'Coherence and Cohesion',
  LR: 'Lexical Resource',
  GRA: 'Grammatical Range and Accuracy'
};

/** Nhóm tiêu chí của mã lỗi (`criterion` của mã lỗi: TASK = TA ở Task 1, TR ở Task 2). */
export const ERROR_GROUP_LABEL: Record<string, string> = {
  TASK: 'TA / TR',
  CC: 'CC',
  LR: 'LR',
  GRA: 'GRA'
};

/**
 * Đường dẫn file do backend phục vụ (`/uploads/…`): khi frontend và backend khác origin (lúc dev)
 * thì gắn thêm NEXT_PUBLIC_API_BASE_URL; link http(s) đầy đủ giữ nguyên.
 */
export function fileUrl(path: string): string {
  return path.startsWith('/') ? `${process.env.NEXT_PUBLIC_API_BASE_URL ?? ''}${path}` : path;
}

// ─── Đường dẫn trang sinh viên ────────────────────────────────────────────────
// Thông báo của server cũng trỏ tới /sinh-vien/bai-tap/{assignmentId} (docs/05 mục A.9).

export const studentAssignmentPath = (id: number) => `/sinh-vien/bai-tap/${id}`;

/** Kết quả một phần: lần nộp `versionNo` (bỏ trống = lần gần nhất đã trả bài) — Prompt 7 D, E. */
export const resultPath = (assignmentId: number, partId: number, versionNo?: number | null) =>
  `/sinh-vien/bai-tap/${assignmentId}/ket-qua?phan=${partId}${versionNo ? `&lan=${versionNo}` : ''}`;

/** Màn làm bài: mở sẵn phần `partId` (bỏ trống = phần đầu tiên). */
export const writingPath = (assignmentId: number, partId?: number | null) =>
  `/sinh-vien/lam-bai/${assignmentId}${partId ? `?phan=${partId}` : ''}`;

// ─── Đường dẫn chấm bài ───────────────────────────────────────────────────────
// Bộ lọc hàng đợi đi theo URL sang màn chấm để nút "Trước / Sau" đi đúng thứ tự hàng đợi.

const QUEUE_KEYS = ['status', 'assignmentId', 'partId', 'classId', 'keyword'] as const;
type QueueFilter = Partial<Record<(typeof QUEUE_KEYS)[number], string | number>>;

export function queueQueryString(filter: QueueFilter): string {
  const qs = new URLSearchParams();
  for (const key of QUEUE_KEYS) {
    const value = filter[key];
    if (value !== undefined && value !== '') qs.set(key, String(value));
  }
  return qs.toString();
}

export const gradingPath = (submissionId: number, filter: QueueFilter = {}) => {
  const qs = queueQueryString(filter);
  return `/giao-vien/cham-bai/${submissionId}${qs ? `?${qs}` : ''}`;
};

// ─── Chấm bài ─────────────────────────────────────────────────────────────────

/** BR-19: lý do chấm 0 (docs/08 mục 3.3). */
export const ZERO_REASON_LABEL: Record<'NO_ATTEMPT' | 'NOT_ENGLISH' | 'MEMORISED', string> = {
  NO_ATTEMPT: 'Không làm bài',
  NOT_ENGLISH: 'Không viết bằng tiếng Anh',
  MEMORISED: 'Bài học thuộc hoàn toàn'
};
