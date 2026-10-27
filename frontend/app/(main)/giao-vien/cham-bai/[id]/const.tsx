import type { AnnotationView } from '@/components/EssayAnnotator';
import type { ErrorCodeDto, GradingCriterion, Resolution, ZeroReason } from '@/lib/types/grading';
import { categoryOf } from '@/lib/utils/annotations';

import type { LocalInline } from './useGradeForm';

/** BR-19: lý do chấm 0 (docs/08 mục 3.3). */
export const ZERO_REASON_LABEL: Record<ZeroReason, string> = {
  NO_ATTEMPT: 'Không làm bài',
  NOT_ENGLISH: 'Không viết bằng tiếng Anh',
  MEMORISED: 'Bài học thuộc hoàn toàn'
};

/** So sánh 2 lần nộp: lỗi của lần cũ ở lần sau (docs/08 mục 4.3). Class viết nguyên văn để Tailwind sinh được. */
export const RESOLUTION_META: Record<Resolution, { label: string; text: string; dot: string }> = {
  FIXED: { label: 'Đã sửa', text: 'text-success', dot: 'bg-success' },
  CHANGED: { label: 'Đổi khác gợi ý', text: 'text-warning', dot: 'bg-warning' },
  NOT_FIXED: { label: 'Chưa sửa', text: 'text-error', dot: 'bg-error' }
};

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
