import type { Role } from '@/lib/constants';

// ─── Quản trị (docs/05 mục A.3) ───────────────────────────────────────────────

export interface UserSummary {
  id: number;
  username: string;
  fullName: string;
  email: string | null;
  role: Role;
  studentCode: string | null;
  active: boolean;
  mustChangePassword: boolean;
  lastLoginAt: string | null;
  createdAt: string;
  /** Số lớp phụ trách — chỉ có ở danh sách giáo viên. */
  classCount: number | null;
}

export interface UserDetail {
  user: UserSummary;
  activeSessions: number;
}

export interface UserListParams {
  keyword?: string;
  role?: Role;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface TeacherFormValues {
  username: string;
  fullName: string;
  email?: string;
}

/** Mật khẩu tạm chỉ trả về đúng một lần trong response tạo / đặt lại. */
export interface CreatedTeacher {
  teacher: UserSummary;
  temporaryPassword: string;
}

export interface SystemStats {
  usersByRole: Partial<Record<Role, number>>;
  lockedUsers: number;
  classrooms: number;
  /** Theo trạng thái bài tập: DRAFT, PUBLISHED, CLOSED… */
  assignmentsByStatus: Record<string, number>;
  submissionsToday: number;
  /** 14 ngày gần nhất theo giờ Việt Nam, ngày cũ trước. `date` dạng YYYY-MM-DD. */
  submissionsByDay: { date: string; count: number }[];
  activeSessions: number;
}

export type AuditAction =
  | 'LOGIN'
  | 'LOGIN_FAILED'
  | 'LOGIN_BLOCKED'
  | 'CREATE_TEACHER'
  | 'LOCK_USER'
  | 'UNLOCK_USER'
  | 'RESET_PASSWORD'
  | 'IMPORT_STUDENTS'
  | 'TCP_IMPORT'
  | 'PUBLISH_ASSIGNMENT'
  | 'RETURN_GRADE'
  | 'REQUEST_REVISION';

export interface AuditLog {
  id: number;
  actorId: number | null;
  actorUsername: string | null;
  actorName: string | null;
  action: AuditAction;
  targetType: string | null;
  targetId: number | null;
  ip: string | null;
  /** JSON tuỳ hành động (số dòng import, tên bài…). */
  detail: Record<string, unknown> | null;
  createdAt: string;
}

export interface AuditLogParams {
  action?: AuditAction;
  ip?: string;
  /** ISO 8601; from ≤ thời điểm < to. */
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}
