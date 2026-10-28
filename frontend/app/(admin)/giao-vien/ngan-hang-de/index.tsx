'use client';

import { useMemo, useState } from 'react';

import { App, Button, Card, Empty, Input, Segmented, Select, Table, Tag } from 'antd';
import { Plus } from 'lucide-react';

import { PageHeader } from '@/components/PageHeader';
import { questionBankApi } from '@/lib/api/questionBank';
import type { QuestionBankListParams, QuestionBankSummary } from '@/lib/types/questionBank';
import type { TaskType } from '@/lib/types/assignment';
import { useServerList } from '@/lib/utils/hooks';

import { buildColumns, PAGE_SIZE, SORT_OPTIONS, TASK_FILTER } from './const';
import { QuestionBankAssignModal } from './QuestionBankAssignModal';
import { QuestionBankDetailDrawer } from './QuestionBankDetailDrawer';
import { QuestionBankFormDrawer } from './QuestionBankFormDrawer';

export default function QuestionBankPage() {
  const { message, modal } = App.useApp();
  const [query, setQuery] = useState<QuestionBankListParams>({ page: 0, size: PAGE_SIZE, sort: 'updatedAt,desc' });
  const { data, total, loading, reload } = useServerList(questionBankApi.list, query);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [assignId, setAssignId] = useState<number | null>(null);
  const [formState, setFormState] = useState<{ open: boolean; itemId: number | null }>({ open: false, itemId: null });

  const openCreate = () => setFormState({ open: true, itemId: null });
  const openEdit = (itemId: number) => {
    setDetailId(null);
    setFormState({ open: true, itemId });
  };
  const openAssign = (itemId: number) => {
    setDetailId(null);
    setAssignId(itemId);
  };

  const remove = (item: QuestionBankSummary) => {
    modal.confirm({
      title: 'Xoá đề khỏi ngân hàng?',
      content: `Đề “${item.title}” và rubric riêng đi kèm sẽ bị xoá. Các bài tập đã dùng đề này không bị ảnh hưởng.`,
      okText: 'Xoá đề',
      cancelText: 'Huỷ',
      okButtonProps: { danger: true },
      onOk: async () => {
        await questionBankApi.remove(item.id);
        if (detailId === item.id) setDetailId(null);
        message.success('Đã xoá đề khỏi ngân hàng');
        reload();
      }
    });
  };

  const columns = useMemo(
    () =>
      buildColumns({
        onView: setDetailId,
        onAssign: openAssign,
        onEdit: openEdit,
        onDelete: remove,
        onTag: (tag) => setQuery((current) => ({ ...current, tag, page: 0 }))
      }),
    // Các callback chỉ cập nhật state hoặc dùng instance ổn định của Ant Design.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [detailId, message, modal, reload]
  );

  const createButton = (
    <Button type="primary" icon={<Plus size={16} />} onClick={openCreate}>
      Thêm đề
    </Button>
  );

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader
        title="Ngân hàng đề"
        description="Lưu đề kèm rubric và tag để tìm kiếm, chỉnh sửa, giao trực tiếp hoặc dùng lại khi soạn bài tập."
        extra={createButton}
      />

      <Card styles={{ body: { padding: 0 } }}>
        <div className="flex flex-wrap items-center gap-2 border-b border-neutral-100 p-4">
          <Segmented
            options={TASK_FILTER}
            value={query.taskType ?? 'all'}
            onChange={(value) =>
              setQuery((current) => ({
                ...current,
                taskType: value === 'all' ? undefined : (value as TaskType),
                page: 0
              }))
            }
          />
          <Input.Search
            allowClear
            placeholder="Tìm theo tên hoặc nội dung đề"
            className="w-full sm:ml-auto sm:w-72"
            onSearch={(keyword) =>
              setQuery((current) => ({ ...current, keyword: keyword.trim() || undefined, page: 0 }))
            }
          />
          <Select
            className="w-full sm:w-40"
            value={query.sort}
            options={SORT_OPTIONS}
            onChange={(sort) => setQuery((current) => ({ ...current, sort, page: 0 }))}
          />
        </div>

        {query.tag && (
          <div className="border-b border-neutral-100 px-4 py-2 text-small text-neutral-600">
            Đang lọc tag:{' '}
            <Tag closable onClose={() => setQuery((current) => ({ ...current, tag: undefined, page: 0 }))}>
              {query.tag}
            </Tag>
          </div>
        )}

        <Table<QuestionBankSummary>
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 820 }}
          locale={{
            emptyText:
              query.keyword || query.taskType || query.tag ? (
                'Không có đề nào khớp bộ lọc'
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Ngân hàng đề đang trống">
                  {createButton}
                </Empty>
              )
          }}
          pagination={{
            current: (query.page ?? 0) + 1,
            pageSize: PAGE_SIZE,
            total,
            showSizeChanger: false,
            hideOnSinglePage: true,
            onChange: (page) => setQuery((current) => ({ ...current, page: page - 1 }))
          }}
        />
      </Card>

      <QuestionBankDetailDrawer
        itemId={detailId}
        onClose={() => setDetailId(null)}
        onAssign={openAssign}
        onEdit={openEdit}
      />
      <QuestionBankAssignModal key={assignId ?? 'closed'} itemId={assignId} onClose={() => setAssignId(null)} />
      <QuestionBankFormDrawer
        key={`${formState.open}-${formState.itemId ?? 'new'}`}
        open={formState.open}
        itemId={formState.itemId}
        onClose={() => setFormState({ open: false, itemId: null })}
        onSaved={(successMessage) => {
          message.success(successMessage);
          setFormState({ open: false, itemId: null });
          reload();
        }}
      />
    </div>
  );
}
