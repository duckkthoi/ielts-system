import { getSubmissionDisplayStatus, SUBMISSION_STATUS_META } from '@/lib/theme/status';
import type { StudentPartDetail } from '@/lib/types/student';

/** Đồng bộ lại giờ server mỗi 5 phút khi đang ở màn làm bài (SD-11). */
export const CLOCK_SYNC_MS = 5 * 60_000;

/** Lỗi nộp nghĩa là trạng thái trên trang đã cũ → tải lại. */
export const RELOAD_CODES = new Set(['VERSION_CONFLICT', 'ALREADY_SUBMITTED', 'DEADLINE_PASSED', 'INVALID_STATE']);

/** Còn soạn được: server cho nộp và phần đang chưa làm / đang làm / cần sửa. */
export function isWritable(part: StudentPartDetail): boolean {
  return (
    part.canSubmit && (part.status === 'NOT_STARTED' || part.status === 'DRAFT' || part.status === 'REVISION_REQUESTED')
  );
}

/** Nhãn tab phần: "Task 1 — … · Đã nộp". */
export function partTabLabel(part: StudentPartDetail): string {
  return `${part.title} · ${SUBMISSION_STATUS_META[getSubmissionDisplayStatus(part.status, part.late, 'student')].label}`;
}
