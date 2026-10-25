// ─── Response chuẩn của backend ──────────────────────────────────────────────
// Mọi API trả cùng một dạng: { success, data, message, totalRecord }.
// Khi lỗi (HTTP status vẫn là mã thật 4xx / 5xx): { success: false, data, errorCode, errorMessage, status, fieldErrors }.

export interface ApiResponse<T = null> {
  success: boolean;
  data: T;
  message: string | null;
  /** Tổng số bản ghi — chỉ có ở API phân trang. */
  totalRecord?: number;
}

export interface ApiErrorBody {
  success: false;
  /** Thường là null; một số lỗi kèm dữ liệu (vd bản nháp trên server khi 409 DRAFT_CONFLICT). */
  data: unknown;
  errorCode?: string;
  errorMessage?: string | null;
  status?: number;
  /** Lỗi validate theo từng trường: { tenTruong: 'thông báo' }. */
  fieldErrors?: Record<string, string> | null;
}

export interface PageParams {
  page?: number;
  size?: number;
  keyword?: string;
}
