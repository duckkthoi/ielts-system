// ─── Lỗi từ API ───────────────────────────────────────────────────────────────
// axiosClient reject bằng ApiError để trang tự xử lý được theo `code` (vd RATE_LIMITED, DRAFT_CONFLICT)
// hoặc gán `fieldErrors` vào form. `message` luôn là chuỗi tiếng Việt hiển thị được.

export class ApiError extends Error {
  readonly code?: string;
  readonly status?: number;
  readonly fieldErrors?: Record<string, string> | null;
  /** Dữ liệu kèm lỗi (thường null), vd bản nháp trên server khi DRAFT_CONFLICT. */
  readonly data?: unknown;

  constructor(
    message: string,
    options: { code?: string; status?: number; fieldErrors?: Record<string, string> | null; data?: unknown } = {}
  ) {
    super(message);
    this.name = 'ApiError';
    this.code = options.code;
    this.status = options.status;
    this.fieldErrors = options.fieldErrors;
    this.data = options.data;
  }
}

/** Lấy message hiển thị được từ lỗi bất kỳ mà một lời gọi API ném ra. */
export function getErrorMessage(error: unknown, fallback = 'Thao tác thất bại'): string {
  return error instanceof Error && error.message ? error.message : fallback;
}
