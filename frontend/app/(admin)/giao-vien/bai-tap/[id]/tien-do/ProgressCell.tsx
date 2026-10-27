'use client';

import { Tooltip } from 'antd';
import clsx from 'clsx';
import { AlertCircle, Clock } from 'lucide-react';

import { formatBand, getSubmissionDisplayStatus, SUBMISSION_STATUS_META } from '@/lib/theme/status';
import type { ProgressCellDto } from '@/lib/types/stats';

// ─── Ô ma trận tiến độ (docs/07 mục 7.2) ──────────────────────────────────────
// Nền + chữ theo trạng thái, band nếu đã trả, đồng hồ nếu nộp muộn, chấm than đỏ nếu quá hạn chưa nộp.
// Có bài nộp thì bấm để mở màn chấm. `pulse` đổi giá trị khi ô vừa được cập nhật realtime.

const GRADABLE = ['SUBMITTED', 'GRADED', 'RETURNED', 'REVISION_REQUESTED'];

export function ProgressCell({
  cell,
  viewing,
  pulse,
  onOpen
}: {
  cell: ProgressCellDto;
  /** Sinh viên đang mở phần này. */
  viewing: boolean;
  pulse: number;
  onOpen: (cell: ProgressCellDto) => void;
}) {
  const display = getSubmissionDisplayStatus(cell.status, cell.late, 'teacher');
  const meta = SUBMISSION_STATUS_META[display];
  const openable = cell.submissionId !== null && GRADABLE.includes(cell.status);

  const details = [
    meta.label,
    cell.currentVersion > 1 ? `lần nộp ${cell.currentVersion}` : null,
    cell.late && display !== 'LATE' ? 'nộp muộn' : null,
    cell.overdue ? 'đã quá hạn' : null,
    cell.score !== null ? `band ${formatBand(cell.score)}` : null,
    viewing ? 'đang mở phần này' : null
  ].filter(Boolean);

  return (
    <Tooltip title={`${details.join(' · ')}${openable ? ' — bấm để mở bài' : ''}`}>
      <button
        // key đổi theo pulse để chạy lại hiệu ứng mỗi lần cập nhật
        key={pulse}
        type="button"
        // Không dùng `disabled` để tooltip vẫn hiện trên ô chưa có bài
        aria-disabled={!openable}
        onClick={() => openable && onOpen(cell)}
        className={clsx(
          'inline-flex h-8 min-w-24 items-center justify-center gap-1 rounded-md border px-2 text-caption font-medium whitespace-nowrap',
          openable ? 'cursor-pointer hover:brightness-95' : 'cursor-default',
          pulse > 0 && 'animate-pulse-once',
          viewing && 'ring-2 ring-presence-online ring-offset-1'
        )}
        style={{ color: meta.text, background: meta.bg, borderColor: meta.border }}
      >
        {cell.late && <Clock size={12} aria-label="Nộp muộn" />}
        {cell.overdue && <AlertCircle size={12} className="text-error" aria-label="Quá hạn" />}
        {cell.score !== null ? (
          <span className="tabular-nums">
            {display === 'REVISION_REQUESTED' ? 'Sửa · ' : ''}
            {formatBand(cell.score)}
          </span>
        ) : (
          meta.label
        )}
        {cell.currentVersion > 1 && <span className="opacity-70">v{cell.currentVersion}</span>}
      </button>
    </Tooltip>
  );
}
