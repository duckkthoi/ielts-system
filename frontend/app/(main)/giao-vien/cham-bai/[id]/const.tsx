import type { ZeroReason } from '@/lib/types/grading';

/** BR-19: lý do chấm 0 (docs/08 mục 3.3). */
export const ZERO_REASON_LABEL: Record<ZeroReason, string> = {
  NO_ATTEMPT: 'Không làm bài',
  NOT_ENGLISH: 'Không viết bằng tiếng Anh',
  MEMORISED: 'Bài học thuộc hoàn toàn'
};

/** Số bài tối đa lấy về để tính vị trí "3/12" và nút Trước / Sau. */
export const QUEUE_WINDOW = 200;
