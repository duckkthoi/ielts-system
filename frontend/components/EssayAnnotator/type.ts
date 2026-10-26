import type { AnnLike, ErrorCategory } from '@/lib/utils/annotations';

/** Một đánh dấu kèm thông tin hiển thị (giáo viên và sinh viên dùng chung). */
export interface AnnotationView extends AnnLike {
  quote: string;
  errorName: string | null;
  /** Nhóm lỗi để tô màu gạch gợn sóng; null khi không phải lỗi có mã. */
  category: ErrorCategory | null;
  criterionLabel: string | null;
}

/** Nút trên thanh công cụ khi bôi đen. */
export type AnnotateAction = 'error' | 'highlight' | 'good' | 'comment' | 'replace' | 'delete' | 'insert';
