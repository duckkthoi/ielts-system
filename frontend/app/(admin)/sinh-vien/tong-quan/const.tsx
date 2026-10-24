import type { StudentTab } from '@/lib/types/student';
import type { StudentActionKind } from '@/lib/utils/studentAction';

export const DEFAULT_TAB: StudentTab = 'TODO';

/** Thứ tự tab + chữ khi tab rỗng (Prompt 7 A). */
export const TABS: { key: StudentTab; label: string; empty: string }[] = [
  { key: 'TODO', label: 'Cần làm', empty: 'Không có bài nào cần làm. Tuyệt vời!' },
  { key: 'DUE_SOON', label: 'Sắp hết hạn', empty: 'Không có bài nào hết hạn trong 24 giờ tới.' },
  { key: 'OVERDUE', label: 'Quá hạn', empty: 'Không có bài nào quá hạn.' },
  { key: 'SUBMITTED', label: 'Đã nộp', empty: 'Chưa có bài nào nộp đủ mọi phần.' },
  { key: 'REVISION', label: 'Cần sửa', empty: 'Không có bài nào cần sửa.' }
];

export const isStudentTab = (value: string | null): value is StudentTab => TABS.some((t) => t.key === value);

export const ACTION_LABEL: Record<StudentActionKind, string> = {
  revise: 'Sửa bài',
  write: 'Làm bài',
  result: 'Xem kết quả',
  view: 'Xem chi tiết'
};
