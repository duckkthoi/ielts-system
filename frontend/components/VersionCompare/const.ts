import type { Resolution } from '@/lib/types/grading';

/** Lỗi của lần cũ ở lần sau (docs/08 mục 4.3). Class viết nguyên văn để Tailwind sinh được. */
export const RESOLUTION_META: Record<Resolution, { label: string; text: string; dot: string }> = {
  FIXED: { label: 'Đã sửa', text: 'text-success', dot: 'bg-success' },
  CHANGED: { label: 'Đổi khác gợi ý', text: 'text-warning', dot: 'bg-warning' },
  NOT_FIXED: { label: 'Chưa sửa', text: 'text-error', dot: 'bg-error' }
};
