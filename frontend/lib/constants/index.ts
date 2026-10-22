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
