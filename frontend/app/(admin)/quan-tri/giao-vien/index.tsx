'use client';

import { useCallback, useMemo, useState } from 'react';

import { App, Button, Card, Input, Select, Table } from 'antd';
import { FileUp, Plus } from 'lucide-react';

import { useAccountActions } from '@/components/AccountActions';
import { PageHeader } from '@/components/PageHeader';
import { adminApi } from '@/lib/api/admin';
import type { CreatedTeacher, UserListParams, UserSummary } from '@/lib/types/admin';
import { useServerList } from '@/lib/utils/hooks';

import { ACTIVE_OPTIONS, buildColumns, PAGE_SIZE } from './const';
import { TeacherFormDrawer } from './TeacherFormDrawer';
import { TeacherImportModal } from './TeacherImportModal';

// ─── Quản trị: danh sách giáo viên (UC-05) + import từ file (UC-05b) ──────────

export default function TeachersPage() {
  const { message } = App.useApp();
  const [query, setQuery] = useState<UserListParams>({ page: 0, size: PAGE_SIZE });
  const [importOpen, setImportOpen] = useState(false);
  const [drawer, setDrawer] = useState<{ open: boolean; editing: UserSummary | null }>({ open: false, editing: null });
  const { data, total, loading, reload } = useServerList(adminApi.listTeachers, query);
  const { renderMenu, showPassword, passwordModal } = useAccountActions(reload);

  const openCreate = useCallback(() => setDrawer({ open: true, editing: null }), []);
  const openEdit = useCallback((user: UserSummary) => setDrawer({ open: true, editing: user }), []);
  const closeDrawer = useCallback(() => setDrawer((d) => ({ ...d, open: false })), []);

  const handleCreated = useCallback(
    (created: CreatedTeacher) => {
      closeDrawer();
      showPassword({
        title: 'Đã tạo tài khoản giáo viên',
        username: created.teacher.username,
        password: created.temporaryPassword
      });
      reload();
    },
    [closeDrawer, showPassword, reload]
  );

  const handleUpdated = useCallback(() => {
    closeDrawer();
    message.success('Đã lưu thông tin giáo viên');
    reload();
  }, [closeDrawer, message, reload]);

  const columns = useMemo(() => buildColumns((user) => renderMenu(user, openEdit)), [renderMenu, openEdit]);

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader
        title="Giáo viên"
        description="Tạo tài khoản giáo viên (từng người hoặc import từ file), sửa thông tin, khoá và đặt lại mật khẩu."
        extra={
          <div className="flex flex-wrap gap-2">
            <Button icon={<FileUp size={16} />} onClick={() => setImportOpen(true)}>
              Import từ file
            </Button>
            <Button type="primary" icon={<Plus size={16} />} onClick={openCreate}>
              Thêm giáo viên
            </Button>
          </div>
        }
      />
      <Card styles={{ body: { padding: 0 } }}>
        <div className="flex flex-wrap gap-2 border-b border-neutral-100 p-4">
          <Input.Search
            allowClear
            placeholder="Tìm theo tên đăng nhập, họ tên, email"
            className="w-full sm:w-80"
            onSearch={(keyword) => setQuery((q) => ({ ...q, keyword: keyword.trim() || undefined, page: 0 }))}
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
          locale={{ emptyText: query.keyword ? 'Không có giáo viên nào khớp' : 'Chưa có giáo viên nào' }}
          pagination={{
            current: (query.page ?? 0) + 1,
            pageSize: PAGE_SIZE,
            total,
            showSizeChanger: false,
            showTotal: (n) => `${n} giáo viên`,
            onChange: (page) => setQuery((q) => ({ ...q, page: page - 1 }))
          }}
        />
      </Card>
      <TeacherFormDrawer
        open={drawer.open}
        editing={drawer.editing}
        onClose={closeDrawer}
        onCreated={handleCreated}
        onUpdated={handleUpdated}
      />
      <TeacherImportModal open={importOpen} onClose={() => setImportOpen(false)} onImported={reload} />
      {passwordModal}
    </div>
  );
}
