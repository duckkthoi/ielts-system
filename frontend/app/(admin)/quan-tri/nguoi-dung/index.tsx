'use client';

import { useCallback, useMemo, useState } from 'react';

import { Card, Input, Select, Table } from 'antd';

import { useAccountActions } from '@/components/AccountActions';
import { PageHeader } from '@/components/PageHeader';
import { adminApi } from '@/lib/api/admin';
import type { Role } from '@/lib/constants';
import type { UserListParams, UserSummary } from '@/lib/types/admin';
import { useServerList } from '@/lib/utils/hooks';

import { ACTIVE_OPTIONS, buildColumns, PAGE_SIZE, ROLE_OPTIONS } from './const';
import { UserDetailDrawer } from './UserDetailDrawer';

// ─── Quản trị: tra cứu mọi tài khoản (UC-06) ──────────────────────────────────
// Thêm giáo viên ở trang Giáo viên; sinh viên được tạo khi giáo viên import vào lớp.

export default function UsersPage() {
  const [query, setQuery] = useState<UserListParams>({ page: 0, size: PAGE_SIZE });
  const [detailId, setDetailId] = useState<number | null>(null);
  const { data, total, loading, reload } = useServerList(adminApi.listUsers, query);
  const { renderMenu, passwordModal } = useAccountActions(reload);

  const columns = useMemo(() => buildColumns((user) => renderMenu(user)), [renderMenu]);
  const closeDetail = useCallback(() => setDetailId(null), []);

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader title="Người dùng" description="Tra cứu tài khoản, khoá / mở khoá và đặt lại mật khẩu." />
      <Card styles={{ body: { padding: 0 } }}>
        <div className="flex flex-wrap gap-2 border-b border-neutral-100 p-4">
          <Input.Search
            allowClear
            placeholder="Tìm theo tên đăng nhập, họ tên, mã sinh viên, email"
            className="w-full sm:w-96"
            onSearch={(keyword) => setQuery((q) => ({ ...q, keyword: keyword.trim() || undefined, page: 0 }))}
          />
          <Select
            allowClear
            placeholder="Vai trò"
            className="w-40"
            options={ROLE_OPTIONS}
            onChange={(role?: Role) => setQuery((q) => ({ ...q, role, page: 0 }))}
          />
          <Select
            allowClear
            placeholder="Trạng thái"
            className="w-40"
            options={ACTIVE_OPTIONS}
            onChange={(active?: boolean) => setQuery((q) => ({ ...q, active, page: 0 }))}
          />
        </div>
        <Table<UserSummary>
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 640 }}
          rowClassName="cursor-pointer"
          onRow={(row) => ({ onClick: () => setDetailId(row.id) })}
          locale={{ emptyText: 'Không có tài khoản nào khớp bộ lọc' }}
          pagination={{
            current: (query.page ?? 0) + 1,
            pageSize: PAGE_SIZE,
            total,
            showSizeChanger: false,
            showTotal: (n) => `${n} tài khoản`,
            onChange: (page) => setQuery((q) => ({ ...q, page: page - 1 }))
          }}
        />
      </Card>
      <UserDetailDrawer userId={detailId} onClose={closeDetail} />
      {passwordModal}
    </div>
  );
}
