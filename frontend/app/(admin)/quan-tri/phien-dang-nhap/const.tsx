import { Button, Dropdown, Tag, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { LogOut, MoreHorizontal, UserX } from 'lucide-react';

import { StatusTag } from '@/components/StatusTag';
import { ROLE_LABEL } from '@/lib/constants';
import { REVOKE_REASON_LABEL, SESSION_STATUS_META } from '@/lib/theme/status';
import type { SessionRow, SessionStatus } from '@/lib/types/admin';
import { formatDateTime, formatRelative } from '@/lib/utils/date';

export const PAGE_SIZE = 20;

export const STATUS_OPTIONS = (Object.keys(SESSION_STATUS_META) as SessionStatus[]).map((value) => ({
  value,
  label: SESSION_STATUS_META[value].label
}));

export const ROLE_OPTIONS = Object.entries(ROLE_LABEL).map(([value, label]) => ({ value, label }));

/** Trường sắp xếp server cho phép (docs/05 A.3). */
export const SORT_FIELDS = new Set(['createdAt', 'lastUsedAt', 'expiresAt', 'username']);

export function buildColumns(
  onRevoke: (row: SessionRow) => void,
  onRevokeAll: (row: SessionRow) => void
): ColumnsType<SessionRow> {
  return [
    {
      title: 'Người dùng',
      key: 'username',
      sorter: true,
      render: (_, row) => (
        <span className="block min-w-0">
          <span className="block font-medium text-neutral-900">
            {row.fullName}
            {row.current && (
              <Tag color="blue" className="ml-2">
                Phiên của bạn
              </Tag>
            )}
          </span>
          <span className="font-mono text-caption text-neutral-500">
            {row.username} · {ROLE_LABEL[row.role]}
          </span>
        </span>
      )
    },
    {
      title: 'Thiết bị',
      key: 'device',
      responsive: ['md'],
      render: (_, row) => (
        <span className="block">
          <span className="block text-neutral-700">{row.device}</span>
          <span className="font-mono text-caption text-neutral-500">{row.ipMasked ?? '—'}</span>
        </span>
      )
    },
    {
      title: 'Đăng nhập',
      dataIndex: 'createdAt',
      key: 'createdAt',
      sorter: true,
      responsive: ['lg'],
      render: (value: string) => <span className="tabular-nums">{formatDateTime(value)}</span>
    },
    {
      title: 'Hoạt động lần cuối',
      dataIndex: 'lastUsedAt',
      key: 'lastUsedAt',
      sorter: true,
      defaultSortOrder: 'descend',
      render: (value: string) => (
        <Tooltip title={formatDateTime(value)}>
          <span className="tabular-nums">{formatRelative(value)}</span>
        </Tooltip>
      )
    },
    {
      title: 'Hết hạn',
      dataIndex: 'expiresAt',
      key: 'expiresAt',
      sorter: true,
      responsive: ['xl'],
      render: (value: string) => <span className="tabular-nums">{formatDateTime(value)}</span>
    },
    {
      title: 'Trạng thái',
      key: 'status',
      render: (_, row) => (
        <span className="flex flex-col items-start gap-0.5">
          <StatusTag meta={SESSION_STATUS_META[row.status]} size="sm" />
          {row.revokeReason && (
            <span className="text-caption text-neutral-500">
              {REVOKE_REASON_LABEL[row.revokeReason] ?? row.revokeReason}
            </span>
          )}
        </span>
      )
    },
    {
      title: '',
      key: 'actions',
      width: 56,
      align: 'right',
      render: (_, row) => (
        <Dropdown
          trigger={['click']}
          placement="bottomRight"
          menu={{
            items: [
              {
                key: 'one',
                icon: <LogOut size={14} />,
                label: 'Thu hồi phiên này',
                disabled: row.status !== 'ACTIVE' || row.current
              },
              { key: 'all', icon: <UserX size={14} />, label: 'Thu hồi mọi phiên của tài khoản', danger: true }
            ],
            onClick: ({ key }) => (key === 'one' ? onRevoke(row) : onRevokeAll(row))
          }}
        >
          <Button type="text" icon={<MoreHorizontal size={16} />} aria-label="Thao tác với phiên" />
        </Dropdown>
      )
    }
  ];
}
