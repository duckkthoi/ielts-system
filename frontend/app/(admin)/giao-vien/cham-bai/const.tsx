import { Button, Tag, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import Link from 'next/link';

import { BandBadge } from '@/components/BandBadge';
import { StatusTag } from '@/components/StatusTag';
import { gradingPath } from '@/lib/constants';
import { getSubmissionDisplayStatus, SUBMISSION_STATUS_META } from '@/lib/theme/status';
import type { QueueParams, QueueRow, QueueStatus } from '@/lib/types/grading';
import { formatDateTime } from '@/lib/utils/date';

export const PAGE_SIZE = 20;

export const STATUS_OPTIONS: { value: QueueStatus; label: string }[] = [
  { value: 'SUBMITTED', label: 'Chờ chấm' },
  { value: 'GRADED', label: 'Đang chấm' },
  { value: 'RETURNED', label: 'Đã trả' },
  { value: 'REVISION_REQUESTED', label: 'Cần sửa' }
];

export const isQueueStatus = (v: string | null): v is QueueStatus => STATUS_OPTIONS.some((o) => o.value === v);

export function buildColumns(filter: QueueParams): ColumnsType<QueueRow> {
  return [
    {
      title: 'Sinh viên',
      key: 'student',
      render: (_, row) => (
        <span className="flex flex-col">
          <span className="font-semibold text-neutral-900">{row.student.fullName}</span>
          <span className="font-mono text-small text-neutral-500">{row.student.studentCode}</span>
        </span>
      )
    },
    {
      title: 'Bài tập / Phần',
      key: 'assignment',
      render: (_, row) => (
        <span className="flex flex-col">
          <span className="text-neutral-900">{row.assignmentTitle}</span>
          <span className="text-small text-neutral-500">{row.partTitle}</span>
        </span>
      )
    },
    {
      title: 'Lần nộp',
      dataIndex: 'currentVersion',
      width: 90,
      align: 'center',
      render: (v: number) =>
        v > 1 ? (
          <Tooltip title="Bài nộp lại sau khi yêu cầu sửa">
            <Tag color="blue" className="m-0">
              v{v}
            </Tag>
          </Tooltip>
        ) : (
          <Tag className="m-0">v{v}</Tag>
        )
    },
    {
      title: 'Nộp lúc',
      dataIndex: 'submittedAt',
      width: 160,
      render: (at: string, row) => (
        <span className="flex flex-col items-start gap-1">
          <span className="tabular-nums">{formatDateTime(at)}</span>
          {row.late && <StatusTag meta={SUBMISSION_STATUS_META.LATE} size="sm" />}
        </span>
      )
    },
    {
      title: 'Số từ',
      dataIndex: 'wordCount',
      width: 80,
      align: 'right',
      responsive: ['md'],
      render: (n: number) => <span className="tabular-nums">{n}</span>
    },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 130,
      render: (_, row) => (
        <StatusTag meta={SUBMISSION_STATUS_META[getSubmissionDisplayStatus(row.status, false, 'teacher')]} size="sm" />
      )
    },
    {
      title: 'Band',
      dataIndex: 'band',
      width: 70,
      align: 'center',
      responsive: ['lg'],
      render: (band: number | null) => <BandBadge band={band} size="sm" />
    },
    {
      title: '',
      key: 'action',
      width: 90,
      align: 'right',
      render: (_, row) => (
        <Link href={gradingPath(row.submissionId, filter)}>
          <Button type={row.status === 'SUBMITTED' || row.status === 'GRADED' ? 'primary' : 'default'} size="small">
            {row.status === 'SUBMITTED' || row.status === 'GRADED' ? 'Chấm' : 'Xem'}
          </Button>
        </Link>
      )
    }
  ];
}
