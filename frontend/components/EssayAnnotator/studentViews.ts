import type { StudentInlineComment } from '@/lib/types/student';
import { categoryOf } from '@/lib/utils/annotations';

import type { AnnotationView } from './type';

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
