// ─── Trạng thái → nhãn + màu ─────────────────────────────────────────────────
// Nguồn: docs/07-design-system.md mục 2.4 – 2.6. Mọi tag / ô / biểu đồ lấy màu qua đây.

import type { NotificationType } from '@/lib/types/notification';

import { BRAND, SEMANTIC } from './tokens';
import type { ColorSet, SolidColorSet } from './tokens';

// ─── Bài nộp ──────────────────────────────────────────────────────────────────

export const SUBMISSION_STATUS = {
  NOT_STARTED: 'NOT_STARTED',
  DRAFT: 'DRAFT',
  SUBMITTED: 'SUBMITTED',
  GRADED: 'GRADED',
  RETURNED: 'RETURNED',
  REVISION_REQUESTED: 'REVISION_REQUESTED'
} as const;

export type SubmissionStatus = (typeof SUBMISSION_STATUS)[keyof typeof SUBMISSION_STATUS];

/** Trạng thái hiển thị: thêm LATE (SUBMITTED + is_late), không có trong DB. */
export type SubmissionDisplayStatus = SubmissionStatus | 'LATE';

export const SUBMISSION_STATUS_META: Record<SubmissionDisplayStatus, SolidColorSet & { label: string }> = {
  NOT_STARTED: { label: 'Chưa làm', text: '#475569', bg: '#F1F5F9', border: '#E2E8F0', solid: '#CBD5E1' },
  DRAFT: { label: 'Đang làm', text: '#0369A1', bg: '#F0F9FF', border: '#BAE6FD', solid: '#7DD3FC' },
  SUBMITTED: { label: 'Đã nộp', text: '#1C44C2', bg: '#EFF4FF', border: '#BFD3FE', solid: '#3B6FF6' },
  LATE: { label: 'Nộp muộn', text: '#C2410C', bg: '#FFF7ED', border: '#FED7AA', solid: '#FB923C' },
  GRADED: { label: 'Đang chấm', text: '#6D28D9', bg: '#F5F3FF', border: '#DDD6FE', solid: '#A78BFA' },
  RETURNED: { label: 'Đã trả bài', text: '#15803D', bg: '#F0FDF4', border: '#BBF7D0', solid: '#22C55E' },
  REVISION_REQUESTED: { label: 'Cần sửa', text: '#B91C1C', bg: '#FEF2F2', border: '#FECACA', solid: '#EF4444' }
};

/**
 * Trạng thái hiển thị của một bài nộp.
 * - Đã nộp muộn → LATE.
 * - Phía sinh viên: GRADED hiển thị như SUBMITTED (điểm chưa công bố — BR-13).
 */
export function getSubmissionDisplayStatus(
  status: SubmissionStatus,
  isLate: boolean,
  viewer: 'teacher' | 'student'
): SubmissionDisplayStatus {
  const shown = viewer === 'student' && status === SUBMISSION_STATUS.GRADED ? SUBMISSION_STATUS.SUBMITTED : status;
  if (shown === SUBMISSION_STATUS.SUBMITTED && isLate) return 'LATE';
  return shown;
}

// ─── Bài tập ──────────────────────────────────────────────────────────────────

export const ASSIGNMENT_STATUS = {
  DRAFT: 'DRAFT',
  PUBLISHED: 'PUBLISHED',
  CLOSED: 'CLOSED'
} as const;

export type AssignmentStatus = (typeof ASSIGNMENT_STATUS)[keyof typeof ASSIGNMENT_STATUS];

export const ASSIGNMENT_STATUS_META: Record<AssignmentStatus, ColorSet & { label: string }> = {
  DRAFT: { label: 'Nháp', text: '#475569', bg: '#F1F5F9', border: '#E2E8F0' },
  PUBLISHED: { label: 'Đang mở', text: '#1C44C2', bg: '#EFF4FF', border: '#BFD3FE' },
  CLOSED: { label: 'Đã đóng', text: '#334155', bg: '#E2E8F0', border: '#CBD5E1' }
};

// ─── Band IELTS ───────────────────────────────────────────────────────────────

export interface BandLevel {
  key: 'weak' | 'average' | 'fair' | 'good' | 'excellent';
  label: string;
  /** Band nhỏ nhất của mức (bao gồm). */
  min: number;
  text: string;
  bg: string;
  solid: string;
}

/** Sắp xếp giảm dần theo `min` để tìm mức đầu tiên thoả. */
export const BAND_LEVELS: readonly BandLevel[] = [
  { key: 'excellent', label: 'Xuất sắc', min: 8, text: '#0F766E', bg: '#F0FDFA', solid: '#14B8A6' },
  { key: 'good', label: 'Tốt', min: 7, text: '#15803D', bg: '#F0FDF4', solid: '#22C55E' },
  { key: 'fair', label: 'Khá', min: 6, text: '#A16207', bg: '#FEFCE8', solid: '#EAB308' },
  { key: 'average', label: 'Trung bình', min: 5, text: '#C2410C', bg: '#FFF7ED', solid: '#F97316' },
  { key: 'weak', label: 'Yếu', min: 0, text: '#B91C1C', bg: '#FEF2F2', solid: '#EF4444' }
];

export function getBandLevel(band: number): BandLevel {
  return BAND_LEVELS.find((level) => band >= level.min) ?? BAND_LEVELS[BAND_LEVELS.length - 1];
}

/** Band luôn hiển thị 1 chữ số thập phân ("6.0"); null → "—". */
export function formatBand(band: number | null | undefined): string {
  return band === null || band === undefined ? '—' : band.toFixed(1);
}

// ─── Thông báo ────────────────────────────────────────────────────────────────
// Màu ô icon trong chuông / trang Thông báo theo loại (docs/05 mục A.9).

export const NOTIFICATION_TYPE_META: Record<NotificationType, ColorSet & { label: string }> = {
  ASSIGNMENT_PUBLISHED: { label: 'Bài tập mới', text: BRAND[600], bg: BRAND[50], border: BRAND[200] },
  SUBMISSION_RECEIVED: { label: 'Bài nộp mới', ...SEMANTIC.info },
  GRADE_RETURNED: { label: 'Đã có kết quả', ...SEMANTIC.success },
  REVISION_REQUESTED: { label: 'Cần sửa bài', ...SEMANTIC.error },
  DEADLINE_REMINDER: { label: 'Sắp đến hạn', ...SEMANTIC.warning },
  MANUAL_REMINDER: { label: 'Nhắc nhở', ...SEMANTIC.warning }
};
