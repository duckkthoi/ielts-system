'use client';

import { Button, Tooltip } from 'antd';
import clsx from 'clsx';
import { ClipboardPaste, Eye, EyeOff, WifiOff } from 'lucide-react';

import { StatusTag } from '@/components/StatusTag';
import { EXAM_STATUS_META, getSubmissionDisplayStatus, SUBMISSION_STATUS_META } from '@/lib/theme/status';
import type { MonitorRow } from '@/lib/types/exam';
import type { ProgressCellDto } from '@/lib/types/stats';
import { formatExamClock } from '@/lib/utils/exam';

// ─── Một ô (sinh viên × phần) của màn giám sát ────────────────────────────────
// Trạng thái bài làm; phần ở chế độ thi thì thêm trạng thái thi, đồng hồ, số lần rời trang / dán / mất kết nối.
// Phần còn đang làm → nút "Xem" mở xem trực tiếp (BR-26).

const WORKING = new Set(['NOT_STARTED', 'DRAFT', 'REVISION_REQUESTED']);

function Counter({ icon, count, label }: { icon: React.ReactNode; count: number; label: string }) {
  if (count === 0) return null;
  return (
    <Tooltip title={label}>
      <span className="inline-flex items-center gap-0.5 rounded bg-warning-bg px-1 text-caption font-medium text-warning tabular-nums">
        {icon}
        {count}
      </span>
    </Tooltip>
  );
}

export function MonitorCell({
  cell,
  exam,
  viewing,
  now,
  onOpen
}: {
  cell: ProgressCellDto | undefined;
  exam: MonitorRow | undefined;
  /** Sinh viên đang mở đúng phần này. */
  viewing: boolean;
  /** Giờ server (ms), trang tự tick mỗi giây. */
  now: number;
  onOpen: () => void;
}) {
  const status = cell?.status ?? 'NOT_STARTED';
  const working = WORKING.has(status);
  const clock = exam && !exam.finishedAt ? formatExamClock(exam.endsAt, now) : null;

  return (
    <div
      className={clsx(
        'flex min-w-40 flex-col items-start gap-1 rounded-md px-1.5 py-1',
        viewing && 'ring-1 ring-success-border'
      )}
    >
      <div className="flex items-center gap-1.5">
        <StatusTag
          meta={SUBMISSION_STATUS_META[getSubmissionDisplayStatus(status, cell?.late ?? false, 'teacher')]}
          size="sm"
        />
        {working && (
          <Button size="small" type="link" className="px-0!" icon={<Eye size={14} />} onClick={onOpen}>
            Xem
          </Button>
        )}
      </div>
      {exam && (
        <div className="flex flex-wrap items-center gap-1">
          <StatusTag meta={EXAM_STATUS_META[exam.status]} size="sm" />
          {clock && (
            <span className={clsx('text-caption tabular-nums', clock.overtime ? 'text-error' : 'text-neutral-500')}>
              {clock.text}
            </span>
          )}
          <Counter
            icon={<EyeOff size={11} />}
            count={exam.tabHiddenCount}
            label="Số lần rời khỏi trang / cửa sổ làm bài"
          />
          <Counter icon={<ClipboardPaste size={11} />} count={exam.pasteCount} label="Số lần dán văn bản" />
          <Counter icon={<WifiOff size={11} />} count={exam.disconnectCount} label="Số lần mất kết nối" />
        </div>
      )}
    </div>
  );
}
