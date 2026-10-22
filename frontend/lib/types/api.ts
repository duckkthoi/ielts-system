// ─── Response chuẩn của backend ──────────────────────────────────────────────
// Mọi API trả cùng một dạng: { success, data, message, totalRecord }.
// Khi lỗi: { success: false, data: null, message, code, fieldErrors }.

export interface ApiResponse<T = null> {
  success: boolean;
  data: T;
  message: string | null;
  /** Tổng số bản ghi — chỉ có ở API phân trang. */
  totalRecord?: number;
}

export interface ApiErrorBody {
  success: false;
  data: null;
  message: string | null;
  code?: string;
  status?: number;
  /** Lỗi validate theo từng trường: { tenTruong: 'thông báo' }. */
  fieldErrors?: Record<string, string> | null;
}

export interface PageParams {
  page?: number;
  size?: number;
  keyword?: string;
}
