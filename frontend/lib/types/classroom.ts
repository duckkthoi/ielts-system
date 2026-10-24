// ─── Lớp học + import sinh viên (docs/05 mục A.4) ─────────────────────────────

export interface ClassroomSummary {
  id: number;
  code: string;
  name: string;
  description: string | null;
  memberCount: number;
  /** Số bài tập đang mở được giao cho lớp. */
  openAssignmentCount: number;
  createdAt: string;
}

export interface ClassroomFormValues {
  code: string;
  name: string;
  description?: string;
}

export interface MemberDto {
  studentId: number;
  studentCode: string;
  fullName: string;
  email: string | null;
  active: boolean;
  mustChangePassword: boolean;
  joinedAt: string;
}

export interface AddMembersResult {
  added: number;
  alreadyMember: number;
  /** Mã không tồn tại hoặc không phải tài khoản sinh viên. */
  notFound: string[];
}

export interface ImportRowError {
  /** Số dòng như Excel (dòng tiêu đề = 1). */
  row: number;
  studentCode: string | null;
  reason: string;
}

export interface ImportReport {
  totalRows: number;
  created: number;
  existing: number;
  skipped: number;
  errors: ImportRowError[];
  emailQueued: boolean;
  /** null nếu không có tài khoản mới. */
  credentialsToken: string | null;
  credentialsExpiresAt: string | null;
}

export interface ListParams {
  keyword?: string;
  page?: number;
  size?: number;
}
