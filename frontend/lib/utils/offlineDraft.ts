// ─── Nháp offline: quy tắc thuần (docs/06 mục 7, SD-06) ───────────────────────
// Hàm thuần, không import alias để `node --test` chạy trực tiếp. Phần đọc / ghi IndexedDB ở lib/offlineDraftStore.ts.

/** Bản nháp giữ trên máy: nội dung + `draftVersion` của server lúc bắt đầu sửa (để biết server có đổi sau đó không). */
export interface LocalDraft {
  content: string;
  baseDraftVersion: number;
  updatedAt: number;
}

/**
 * Mở lại màn làm bài mà trên máy còn bản chưa gửi được:
 * - none: không có / trùng bản server → dùng bản server;
 * - restore: server không đổi từ lúc sửa trên máy → dùng bản trên máy rồi lưu lên;
 * - conflict: server cũng đã đổi (tab / máy khác) → hỏi sinh viên chọn bản.
 */
export function decideRestore(
  local: LocalDraft | null,
  server: { content: string; draftVersion: number }
): 'none' | 'restore' | 'conflict' {
  if (!local || local.content === server.content) return 'none';
  return local.baseDraftVersion === server.draftVersion ? 'restore' : 'conflict';
}

/** Lỗi do mạng (không có phản hồi) hoặc proxy không tới được backend → coi là ngoại tuyến, không phải lỗi nghiệp vụ. */
export function isNetworkFailure(status: number | undefined): boolean {
  return status === undefined || status === 502 || status === 503 || status === 504;
}

/** Khoá theo người dùng + phần: máy dùng chung không lẫn nháp của người khác. */
export const offlineDraftKey = (userId: number, partId: number) => `${userId}:${partId}`;
