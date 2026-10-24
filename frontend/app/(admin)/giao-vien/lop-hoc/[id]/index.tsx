'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';

import { App, Button, Card, Empty, Input, Result, Skeleton, Table } from 'antd';
import { ArrowLeft, Pencil, UploadCloud, UserPlus } from 'lucide-react';
import Link from 'next/link';
import { useParams } from 'next/navigation';

import { ClassFormDrawer } from '@/components/ClassFormDrawer';
import { PageHeader } from '@/components/PageHeader';
import { classroomApi } from '@/lib/api/classroom';
import type { ClassroomSummary, ListParams, MemberDto } from '@/lib/types/classroom';
import { useServerList } from '@/lib/utils/hooks';

import { AddMembersModal } from './AddMembersModal';
import { buildMemberColumns, MEMBER_PAGE_SIZE } from './const';
import { ImportModal } from './ImportModal';

// ─── Giáo viên: chi tiết lớp — thành viên, import, thêm / xoá sinh viên (UC-10, UC-11) ─

export default function ClassDetailPage() {
  const { message } = App.useApp();
  const classId = Number(useParams<{ id: string }>().id);
  const [classroom, setClassroom] = useState<ClassroomSummary | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [classVersion, setClassVersion] = useState(0);
  const [query, setQuery] = useState<ListParams>({ page: 0, size: MEMBER_PAGE_SIZE });
  const [importOpen, setImportOpen] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);

  // Lớp của giáo viên khác / không tồn tại → server trả 404 → hiện trang "không tìm thấy".
  useEffect(() => {
    let cancelled = false;
    classroomApi
      .getClass(classId)
      .then((res) => {
        if (!cancelled) setClassroom(res);
      })
      .catch(() => {
        if (!cancelled) setNotFound(true);
      });
    return () => {
      cancelled = true;
    };
  }, [classId, classVersion]);

  const fetchMembers = useCallback((q: ListParams) => classroomApi.listMembers(classId, q), [classId]);
  const { data, total, loading, reload } = useServerList(fetchMembers, query);

  /** Thành viên thay đổi → tải lại bảng và số sinh viên trên tiêu đề. */
  const refresh = useCallback(() => {
    reload();
    setClassVersion((v) => v + 1);
  }, [reload]);

  const handleRemove = useCallback(
    async (member: MemberDto) => {
      try {
        await classroomApi.removeMember(classId, member.studentId);
        message.success(`Đã xoá ${member.fullName} khỏi lớp`);
        refresh();
      } catch {
        // axiosClient đã hiển thị toast lỗi
      }
    },
    [classId, message, refresh]
  );

  const columns = useMemo(() => buildMemberColumns(handleRemove), [handleRemove]);

  const handleSaved = useCallback(
    (saved: ClassroomSummary) => {
      setEditOpen(false);
      setClassroom(saved);
      message.success('Đã lưu lớp');
    },
    [message]
  );

  const back = (
    <Link
      href="/giao-vien/lop-hoc"
      className="mb-3 inline-flex items-center gap-1 text-small text-neutral-500 hover:text-brand-600"
    >
      <ArrowLeft size={14} /> Lớp học
    </Link>
  );

  if (notFound) {
    return (
      <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
        {back}
        <Result
          status="404"
          title="Không tìm thấy lớp"
          subTitle="Lớp không tồn tại hoặc không thuộc quyền quản lý của bạn."
        />
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      {back}
      {!classroom ? (
        <Skeleton active title paragraph={{ rows: 1 }} className="mb-5" />
      ) : (
        <PageHeader
          title={classroom.name}
          description={
            <>
              <span className="font-mono">{classroom.code}</span> · {classroom.memberCount} sinh viên ·{' '}
              {classroom.openAssignmentCount} bài tập đang mở
            </>
          }
          extra={
            <>
              <Button icon={<Pencil size={16} />} onClick={() => setEditOpen(true)}>
                Sửa lớp
              </Button>
              <Button icon={<UserPlus size={16} />} onClick={() => setAddOpen(true)}>
                Thêm sinh viên
              </Button>
              <Button type="primary" icon={<UploadCloud size={16} />} onClick={() => setImportOpen(true)}>
                Import sinh viên
              </Button>
            </>
          }
        />
      )}
      {classroom?.description && <p className="-mt-2 mb-5 max-w-3xl text-neutral-700">{classroom.description}</p>}

      <Card styles={{ body: { padding: 0 } }}>
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-neutral-100 p-4">
          <h2 className="text-h3 font-semibold text-neutral-900">Thành viên ({total})</h2>
          <Input.Search
            allowClear
            placeholder="Tìm theo mã SV, họ tên"
            className="w-full sm:w-72"
            onSearch={(keyword) => setQuery((q) => ({ ...q, keyword: keyword.trim() || undefined, page: 0 }))}
          />
        </div>
        <Table<MemberDto>
          rowKey="studentId"
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 560 }}
          locale={{
            emptyText: query.keyword ? (
              'Không có sinh viên nào khớp'
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Lớp chưa có sinh viên">
                <Button type="primary" icon={<UploadCloud size={16} />} onClick={() => setImportOpen(true)}>
                  Import danh sách sinh viên
                </Button>
              </Empty>
            )
          }}
          pagination={
            total > MEMBER_PAGE_SIZE && {
              current: (query.page ?? 0) + 1,
              pageSize: MEMBER_PAGE_SIZE,
              total,
              showSizeChanger: false,
              onChange: (page) => setQuery((q) => ({ ...q, page: page - 1 }))
            }
          }
        />
      </Card>

      <ImportModal classId={classId} open={importOpen} onClose={() => setImportOpen(false)} onImported={refresh} />
      <AddMembersModal classId={classId} open={addOpen} onClose={() => setAddOpen(false)} onAdded={refresh} />
      <ClassFormDrawer open={editOpen} editing={classroom} onClose={() => setEditOpen(false)} onSaved={handleSaved} />
    </div>
  );
}
