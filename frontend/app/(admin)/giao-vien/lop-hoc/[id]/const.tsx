import { Button, Popconfirm, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Trash2 } from 'lucide-react';

import { AccountStatusTag } from '@/components/AccountStatusTag';
import type { MemberDto } from '@/lib/types/classroom';
import { formatDate } from '@/lib/utils/date';

export const MEMBER_PAGE_SIZE = 50;

/** Định dạng file nhận khi import (docs/05 A.4): tối đa 10 MB, 2000 dòng. */
export const IMPORT_ACCEPT = '.xlsx,.csv';
export const IMPORT_MAX_BYTES = 10 * 1024 * 1024;

export function buildMemberColumns(onRemove: (member: MemberDto) => Promise<void>): ColumnsType<MemberDto> {
  return [
    {
      title: 'Mã SV',
      dataIndex: 'studentCode',
      width: 140,
      render: (value: string) => <span className="font-mono text-small text-neutral-900">{value}</span>
    },
    { title: 'Họ tên', dataIndex: 'fullName', render: (value: string) => <span className="font-medium">{value}</span> },
    { title: 'Email', dataIndex: 'email', responsive: ['md'], render: (value: string | null) => value ?? '—' },
    {
      title: 'Tham gia',
      dataIndex: 'joinedAt',
      width: 120,
      responsive: ['lg'],
      render: (value: string) => formatDate(value)
    },
    {
      title: 'Tài khoản',
      key: 'status',
      render: (_, row) => <AccountStatusTag active={row.active} mustChangePassword={row.mustChangePassword} />
    },
    {
      title: '',
      key: 'actions',
      width: 56,
      align: 'right',
      render: (_, row) => (
        <Popconfirm
          title={`Xoá ${row.fullName} khỏi lớp?`}
          description="Tài khoản sinh viên vẫn giữ nguyên; có thể thêm lại sau."
          okText="Xoá khỏi lớp"
          okButtonProps={{ danger: true }}
          cancelText="Huỷ"
          placement="left"
          onConfirm={() => onRemove(row)}
        >
          <Tooltip title="Xoá khỏi lớp">
            <Button type="text" danger icon={<Trash2 size={16} />} aria-label="Xoá khỏi lớp" />
          </Tooltip>
        </Popconfirm>
      )
    }
  ];
}
