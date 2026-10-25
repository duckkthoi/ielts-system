'use client';

import { useCallback, useState } from 'react';

import { App, Button, Card, Dropdown, Empty, Input, Pagination, Skeleton } from 'antd';
import type { MenuProps } from 'antd';
import { BookOpen, ChevronRight, MoreHorizontal, Plus, Users } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import { ClassFormDrawer } from '@/components/ClassFormDrawer';
import { PageHeader } from '@/components/PageHeader';
import { classroomApi } from '@/lib/api/classroom';
import type { ClassroomSummary, ListParams } from '@/lib/types/classroom';
import { useServerList } from '@/lib/utils/hooks';

import { classPath, PAGE_SIZE } from './const';

// ─── Giáo viên: danh sách lớp (UC-10) ─────────────────────────────────────────

function ClassCard({
  item,
  onEdit,
  onDelete
}: {
  item: ClassroomSummary;
  onEdit: (item: ClassroomSummary) => void;
  onDelete: (item: ClassroomSummary) => void;
}) {
  const items: MenuProps['items'] = [
    { key: 'edit', label: 'Sửa lớp' },
    { key: 'delete', label: 'Xoá lớp', danger: true }
  ];
  const onClick: MenuProps['onClick'] = ({ key, domEvent }) => {
    domEvent.preventDefault();
    if (key === 'edit') onEdit(item);
    if (key === 'delete') onDelete(item);
  };

  return (
    <Card className="h-full transition-shadow hover:shadow-md" styles={{ body: { padding: 20, height: '100%' } }}>
      <div className="flex h-full flex-col">
        <div className="flex items-start justify-between">
          <span className="grid size-10 place-items-center rounded-md bg-brand-50 text-brand-600">
            <Users size={20} />
          </span>
          <Dropdown menu={{ items, onClick }} trigger={['click']} placement="bottomRight">
            <Button type="text" icon={<MoreHorizontal size={16} />} aria-label="Thao tác với lớp" />
          </Dropdown>
        </div>
        <Link href={classPath(item.id)} className="mt-3 block text-neutral-900 hover:text-brand-600">
          <h3 className="text-h3 font-semibold">{item.name}</h3>
        </Link>
        <p className="font-mono text-small text-neutral-500">{item.code}</p>
        {item.description && <p className="mt-2 line-clamp-2 text-small text-neutral-500">{item.description}</p>}
        <div className="mt-auto flex flex-wrap gap-x-4 gap-y-1 pt-4 text-small text-neutral-500">
          <span className="inline-flex items-center gap-1.5">
            <Users size={14} /> {item.memberCount} sinh viên
          </span>
          <span className="inline-flex items-center gap-1.5">
            <BookOpen size={14} /> {item.openAssignmentCount} bài tập đang mở
          </span>
        </div>
        <Link
          href={classPath(item.id)}
          className="mt-3 inline-flex items-center gap-1 self-start font-medium text-brand-600 hover:text-brand-700"
        >
          Mở lớp học <ChevronRight size={14} />
        </Link>
      </div>
    </Card>
  );
}

export default function ClassListPage() {
  const router = useRouter();
  const { modal, message } = App.useApp();
  const [query, setQuery] = useState<ListParams>({ page: 0, size: PAGE_SIZE });
  const [drawer, setDrawer] = useState<{ open: boolean; editing: ClassroomSummary | null }>({
    open: false,
    editing: null
  });
  const { data, total, loading, reload } = useServerList(classroomApi.listClasses, query);

  const openCreate = useCallback(() => setDrawer({ open: true, editing: null }), []);
  const openEdit = useCallback((item: ClassroomSummary) => setDrawer({ open: true, editing: item }), []);
  const closeDrawer = useCallback(() => setDrawer((d) => ({ ...d, open: false })), []);

  const handleSaved = useCallback(
    (saved: ClassroomSummary, created: boolean) => {
      closeDrawer();
      if (created) {
        message.success('Đã tạo lớp. Hãy import danh sách sinh viên.');
        router.push(classPath(saved.id));
      } else {
        message.success('Đã lưu lớp');
        reload();
      }
    },
    [closeDrawer, message, router, reload]
  );

  const confirmDelete = useCallback(
    (item: ClassroomSummary) =>
      modal.confirm({
        title: `Xoá lớp ${item.name}?`,
        content: 'Lớp và danh sách thành viên sẽ bị xoá. Tài khoản sinh viên vẫn giữ nguyên. Không thể hoàn tác.',
        okText: 'Xoá lớp',
        okButtonProps: { danger: true },
        cancelText: 'Huỷ',
        onOk: async () => {
          await classroomApi.deleteClass(item.id);
          message.success('Đã xoá lớp');
          reload();
        }
      }),
    [modal, message, reload]
  );

  const createButton = (
    <Button type="primary" icon={<Plus size={16} />} onClick={openCreate}>
      Tạo lớp
    </Button>
  );

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader
        title="Lớp học"
        description="Quản lý thành viên và import danh sách sinh viên cho các lớp bạn phụ trách."
        extra={createButton}
      />
      <Input.Search
        allowClear
        placeholder="Tìm theo tên hoặc mã lớp"
        className="mb-4 w-full sm:w-80"
        onSearch={(keyword) => setQuery((q) => ({ ...q, keyword: keyword.trim() || undefined, page: 0 }))}
      />
      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 3 }, (_, i) => (
            <Card key={i}>
              <Skeleton active paragraph={{ rows: 3 }} />
            </Card>
          ))}
        </div>
      ) : data.length === 0 ? (
        <Card>
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={
              query.keyword ? 'Không có lớp nào khớp' : 'Chưa có lớp nào. Tạo lớp rồi import danh sách sinh viên.'
            }
          >
            {!query.keyword && createButton}
          </Empty>
        </Card>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {data.map((item) => (
              <ClassCard key={item.id} item={item} onEdit={openEdit} onDelete={confirmDelete} />
            ))}
          </div>
          {total > PAGE_SIZE && (
            <Pagination
              className="mt-6 flex justify-end"
              current={(query.page ?? 0) + 1}
              pageSize={PAGE_SIZE}
              total={total}
              showSizeChanger={false}
              onChange={(page) => setQuery((q) => ({ ...q, page: page - 1 }))}
            />
          )}
        </>
      )}
      <ClassFormDrawer open={drawer.open} editing={drawer.editing} onClose={closeDrawer} onSaved={handleSaved} />
    </div>
  );
}
