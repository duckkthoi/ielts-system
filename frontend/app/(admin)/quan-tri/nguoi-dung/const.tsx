import type { ColumnsType } from 'antd/es/table';

import { AccountStatusTag } from '@/components/AccountStatusTag';
import { ROLE, ROLE_LABEL } from '@/lib/constants';
import type { Role } from '@/lib/constants';
import type { UserSummary } from '@/lib/types/admin';
import { formatDateTime } from '@/lib/utils/date';

export const PAGE_SIZE = 20;

export const ROLE_OPTIONS = (Object.keys(ROLE_LABEL) as Role[]).map((role) => ({
  value: role,
  label: ROLE_LABEL[role]
}));

export const ACTIVE_OPTIONS = [
  { value: true, label: 'Hoạt động' },
  { value: false, label: 'Đã khoá' }
];

const ROLE_TAG_CLASS: Record<Role, string> = {
  [ROLE.ADMIN]: 'bg-neutral-50 text-neutral-700',
  [ROLE.TEACHER]: 'bg-brand-50 text-brand-700',
  [ROLE.STUDENT]: 'bg-info-bg text-info'
};

export function RoleTag({ role }: { role: Role }) {
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-caption font-medium ${ROLE_TAG_CLASS[role]}`}>
      {ROLE_LABEL[role]}
    </span>
  );
}

export function buildColumns(renderActions: (user: UserSummary) => React.ReactNode): ColumnsType<UserSummary> {
  return [
    {
      title: 'Tên đăng nhập',
      dataIndex: 'username',
      render: (value: string) => <span className="font-mono text-small text-neutral-900">{value}</span>
    },
    { title: 'Họ tên', dataIndex: 'fullName', render: (value: string) => <span className="font-medium">{value}</span> },
    { title: 'Vai trò', dataIndex: 'role', render: (role: Role) => <RoleTag role={role} /> },
    { title: 'Email', dataIndex: 'email', responsive: ['lg'], render: (value: string | null) => value ?? '—' },
    {
      title: 'Trạng thái',
      key: 'status',
      render: (_, row) => <AccountStatusTag active={row.active} mustChangePassword={row.mustChangePassword} />
    },
    {
      title: 'Đăng nhập gần nhất',
      dataIndex: 'lastLoginAt',
      responsive: ['md'],
      render: (value: string | null) => (value ? formatDateTime(value) : 'Chưa đăng nhập')
    },
    { title: '', key: 'actions', width: 56, align: 'right', render: (_, row) => renderActions(row) }
  ];
}
