'use client';

import { useCallback, useMemo, useState } from 'react';

import { Button, Card, Empty, Input, Segmented, Table } from 'antd';
import { Plus } from 'lucide-react';
import Link from 'next/link';

import { useAssignmentActions } from '@/components/AssignmentActions';
import { PageHeader } from '@/components/PageHeader';
import { assignmentApi } from '@/lib/api/assignment';
import type { AssignmentStatus } from '@/lib/theme/status';
import type { AssignmentListParams, AssignmentSummary } from '@/lib/types/assignment';
import { useServerList } from '@/lib/utils/hooks';

import { buildColumns, PAGE_SIZE, STATUS_FILTER } from './const';

// ─── Giáo viên: danh sách bài tập (UC-12, UC-14) ──────────────────────────────

const CREATE_PATH = '/giao-vien/bai-tap/tao-moi';

export default function AssignmentListPage() {
  const [query, setQuery] = useState<AssignmentListParams>({ page: 0, size: PAGE_SIZE });
  const { data, total, loading, reload } = useServerList(assignmentApi.list, query);
  const { menuItems, run } = useAssignmentActions(reload);

  const menu = useCallback(
    (row: AssignmentSummary) => ({
      items: menuItems(row),
      onClick: ({ key }: { key: string }) => run(row, key)
    }),
    [menuItems, run]
  );
  const columns = useMemo(() => buildColumns(menu), [menu]);

  const createButton = (
    <Link href={CREATE_PATH}>
      <Button type="primary" icon={<Plus size={16} />}>
        Tạo bài tập
      </Button>
    </Link>
  );

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader
        title="Bài tập"
        description="Soạn đề, giao bài cho lớp và theo dõi tình hình nộp bài."
        extra={createButton}
      />
      <Card styles={{ body: { padding: 0 } }}>
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-neutral-100 p-4">
          <Segmented
            options={STATUS_FILTER}
            value={query.status ?? 'all'}
            onChange={(value) =>
              setQuery((q) => ({ ...q, status: value === 'all' ? undefined : (value as AssignmentStatus), page: 0 }))
            }
          />
          <Input.Search
            allowClear
            placeholder="Tìm theo tên bài tập"
            className="w-full sm:w-72"
            onSearch={(keyword) => setQuery((q) => ({ ...q, keyword: keyword.trim() || undefined, page: 0 }))}
          />
        </div>
        <Table<AssignmentSummary>
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 760 }}
          locale={{
            emptyText:
              query.keyword || query.status ? (
                'Không có bài tập nào khớp bộ lọc'
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có bài tập nào">
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
            onChange: (page) => setQuery((q) => ({ ...q, page: page - 1 }))
          }}
        />
      </Card>
    </div>
  );
}
