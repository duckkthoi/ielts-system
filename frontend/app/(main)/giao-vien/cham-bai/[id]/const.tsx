import type { AnnotationView } from '@/components/EssayAnnotator';
import type { ErrorCodeDto, GradingCriterion } from '@/lib/types/grading';
import { categoryOf } from '@/lib/utils/annotations';

import type { LocalInline } from './useGradeForm';

/** Số bài tối đa lấy về để tính vị trí "3/12" và nút Trước / Sau. */
export const QUEUE_WINDOW = 200;

// ─── Đánh dấu trên bài → dữ liệu hiển thị của EssayAnnotator ─────────────────

/**
 * Tên mã lỗi, nhóm lỗi (màu gạch gợn sóng), nhãn tiêu chí. Không chọn tiêu chí thì hiện tiêu chí cùng nhóm với mã
 * lỗi — đúng tiêu chí server sẽ tự gắn khi lưu.
 */
export function toAnnotationView(ic: LocalInline, codes: ErrorCodeDto[], criteria: GradingCriterion[]): AnnotationView {
  const code = ic.errorCode ? codes.find((c) => c.code.toUpperCase() === ic.errorCode?.toUpperCase()) : undefined;
  const criterion =
    criteria.find((c) => c.id === ic.criterionId) ??
    (code?.criterion ? criteria.find((c) => c.group === code.criterion) : undefined);
  return {
    key: ic.key,
    startOffset: ic.startOffset,
    endOffset: ic.endOffset,
    quote: ic.quote,
    kind: ic.kind,
    errorCode: ic.errorCode,
    comment: ic.comment,
    suggestion: ic.suggestion,
    edit: ic.edit,
    errorName: code?.name ?? null,
    category: ic.kind === 'ERROR' && ic.errorCode ? categoryOf(ic.errorCode, code?.criterion ?? null) : null,
    criterionLabel: criterion ? (criterion.code ?? criterion.name) : null
  };
}
