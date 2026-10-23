export interface LoginFormValues {
  username: string;
  password: string;
}

/** Lỗi hiện ngay trên form đăng nhập: `warning` khi bị chặn tạm (RATE_LIMITED), `error` cho lỗi còn lại. */
export interface LoginError {
  type: 'error' | 'warning';
  message: string;
}
