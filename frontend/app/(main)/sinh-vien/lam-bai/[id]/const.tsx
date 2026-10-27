import type { AnnotationView } from '@/components/EssayAnnotator';
import { getSubmissionDisplayStatus, SUBMISSION_STATUS_META } from '@/lib/theme/status';
import type { StudentInlineComment, StudentPartDetail } from '@/lib/types/student';
import { categoryOf } from '@/lib/utils/annotations';

/** Đồng bộ lại giờ server mỗi 5 phút khi đang ở màn làm bài (SD-11). */
export const CLOCK_SYNC_MS = 5 * 60_000;

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

/**
 * Đánh dấu của một lần nộp đã trả (BR-13) → dữ liệu hiển thị của EssayAnnotator (chỉ đọc). Khi bài đang "Cần sửa",
 * server đã ẩn chữ sửa (BR-21) nên chỉ còn mã lỗi và nhận xét.
 */
export function studentAnnotationViews(list: StudentInlineComment[]): AnnotationView[] {
  return list.map((ic, i) => {
    const group = ic.criterionCode === 'TA' || ic.criterionCode === 'TR' ? 'TASK' : ic.criterionCode;
    return {
      key: `i${i}`,
      startOffset: ic.startOffset,
      endOffset: ic.endOffset,
      quote: ic.quote,
      kind: ic.kind,
      errorCode: ic.errorCode,
      comment: ic.comment,
      suggestion: ic.suggestion,
      edit: ic.edit,
      errorName: ic.errorName,
      category: ic.kind === 'ERROR' && ic.errorCode ? categoryOf(ic.errorCode, group) : null,
      criterionLabel: ic.criterionCode ?? ic.criterionName
    };
  });
}
