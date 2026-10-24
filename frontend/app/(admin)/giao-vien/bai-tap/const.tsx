import { Badge, Button, Dropdown, Progress, Tooltip } from 'antd';
import type { MenuProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { MoreHorizontal } from 'lucide-react';
import Link from 'next/link';

import { assignmentPath } from '@/components/AssignmentActions';
import { CountdownTimer } from '@/components/CountdownTimer';
import { StatusTag } from '@/components/StatusTag';
import { ASSIGNMENT_STATUS_META } from '@/lib/theme/status';
import type { AssignmentStatus } from '@/lib/theme/status';
import type { AssignmentSummary } from '@/lib/types/assignment';
import { formatDateTime } from '@/lib/utils/date';

export const PAGE_SIZE = 20;

/** Bộ lọc trạng thái; `all` = không lọc. */
export const STATUS_FILTER: { value: AssignmentStatus | 'all'; label: string }[] = [
  { value: 'all', label: 'Tất cả' },
  { value: 'DRAFT', label: ASSIGNMENT_STATUS_META.DRAFT.label },
  { value: 'PUBLISHED', label: ASSIGNMENT_STATUS_META.PUBLISHED.label },
  { value: 'CLOSED', label: ASSIGNMENT_STATUS_META.CLOSED.label }
];

export function buildColumns(
  menu: (row: AssignmentSummary) => { items: MenuProps['items']; onClick: MenuProps['onClick'] }
): ColumnsType<AssignmentSummary> {
  return [
    {
      title: 'Tên bài tập',
      key: 'title',
      render: (_, row) => (
        <Link href={assignmentPath(row.id)} className="group block">
          <span className="block font-semibold text-neutral-900 group-hover:text-brand-600">{row.title}</span>
          <span className="text-small text-neutral-500">{row.partCount} phần</span>
        </Link>
      )
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 120,
      render: (status: AssignmentStatus) => <StatusTag meta={ASSIGNMENT_STATUS_META[status]} size="sm" />
    },
    {
      title: 'Hạn nộp',
      dataIndex: 'dueAt',
      width: 190,
      render: (dueAt: string | null, row) =>
        dueAt ? (
          <span className="flex flex-col items-start gap-1">
            <span className="tabular-nums">{formatDateTime(dueAt)}</span>
            {row.status === 'PUBLISHED' && <CountdownTimer dueAt={dueAt} />}
          </span>
        ) : (
          <span className="text-neutral-400">Chưa đặt</span>
        )
    },
    {
      title: 'Đã nộp đủ',
      key: 'completed',
      width: 180,
      responsive: ['md'],
      render: (_, row) =>
        row.status === 'DRAFT' ? (
          <span className="text-neutral-400">—</span>
        ) : (
          <Tooltip title={`${row.completedCount} / ${row.recipientCount} sinh viên đã nộp đủ mọi phần`}>
            <span className="flex items-center gap-2">
              <Progress
                className="m-0 w-20"
                size="small"
                showInfo={false}
                percent={row.recipientCount ? (row.completedCount / row.recipientCount) * 100 : 0}
              />
              <span className="text-small tabular-nums">
                {row.completedCount}/{row.recipientCount}
              </span>
            </span>
          </Tooltip>
        )
    },
    {
      title: 'Chờ chấm',
      dataIndex: 'pendingGradingCount',
      width: 100,
      align: 'center',
      responsive: ['lg'],
      render: (count: number) => <Badge count={count} showZero color={count ? undefined : '#CBD5E1'} />
    },
    {
      title: '',
      key: 'actions',
      width: 210,
      align: 'right',
      render: (_, row) => (
        <span className="inline-flex items-center gap-1">
          {row.status !== 'DRAFT' && (
            <>
              <Link href={`${assignmentPath(row.id)}/tien-do`}>
                <Button size="small">Tiến độ</Button>
              </Link>
              <Link href={`/giao-vien/cham-bai?assignmentId=${row.id}`}>
                <Button size="small">Chấm bài</Button>
              </Link>
            </>
          )}
          <Dropdown menu={menu(row)} trigger={['click']} placement="bottomRight">
            <Button type="text" size="small" icon={<MoreHorizontal size={16} />} aria-label="Thao tác khác" />
          </Dropdown>
        </span>
      )
    }
  ];
}
