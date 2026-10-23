import type { FormRule } from 'antd';
import type { ColumnsType } from 'antd/es/table';

import { AccountStatusTag } from '@/components/AccountStatusTag';
import type { UserSummary } from '@/lib/types/admin';
import { formatDateTime } from '@/lib/utils/date';

export const PAGE_SIZE = 20;

/** Bộ lọc trạng thái: `undefined` = tất cả. */
export const ACTIVE_OPTIONS = [
  { value: true, label: 'Hoạt động' },
  { value: false, label: 'Đã khoá' }
];

/** Cùng quy tắc với server (docs/05 A.3): 3–50 ký tự, chữ không dấu, số và . _ - */
export const USERNAME_RULES: FormRule[] = [
  { required: true, message: 'Vui lòng nhập tên đăng nhập' },
  { min: 3, max: 50, message: 'Tên đăng nhập từ 3 đến 50 ký tự' },
  { pattern: /^[A-Za-z0-9._-]+$/, message: 'Tên đăng nhập chỉ gồm chữ không dấu, số và . _ -' }
];

export const FULL_NAME_RULES: FormRule[] = [
  { required: true, whitespace: true, message: 'Vui lòng nhập họ tên' },
  { max: 150, message: 'Họ tên tối đa 150 ký tự' }
];

export const EMAIL_RULES: FormRule[] = [
  { type: 'email', message: 'Email không hợp lệ' },
  { max: 150, message: 'Email tối đa 150 ký tự' }
];

export function buildColumns(renderActions: (user: UserSummary) => React.ReactNode): ColumnsType<UserSummary> {
  return [
    {
      title: 'Tên đăng nhập',
      dataIndex: 'username',
      render: (value: string) => <span className="font-mono text-small text-neutral-900">{value}</span>
    },
    { title: 'Họ tên', dataIndex: 'fullName', render: (value: string) => <span className="font-medium">{value}</span> },
    { title: 'Email', dataIndex: 'email', responsive: ['md'], render: (value: string | null) => value ?? '—' },
    {
      title: 'Số lớp',
      dataIndex: 'classCount',
      align: 'center',
      width: 90,
      render: (value: number | null) => value ?? 0
    },
    {
      title: 'Trạng thái',
      key: 'status',
      render: (_, row) => <AccountStatusTag active={row.active} mustChangePassword={row.mustChangePassword} />
    },
    {
      title: 'Đăng nhập gần nhất',
      dataIndex: 'lastLoginAt',
      responsive: ['lg'],
      render: (value: string | null) => (value ? formatDateTime(value) : 'Chưa đăng nhập')
    },
    { title: '', key: 'actions', width: 56, align: 'right', render: (_, row) => renderActions(row) }
  ];
}
