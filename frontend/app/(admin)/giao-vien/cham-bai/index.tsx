'use client';

import { useEffect, useMemo, useState } from 'react';

import { Card, Empty, Input, Segmented, Select, Table } from 'antd';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';

import { PageHeader } from '@/components/PageHeader';
import { assignmentApi } from '@/lib/api/assignment';
import { classroomApi } from '@/lib/api/classroom';
import { gradingApi } from '@/lib/api/grading';
import { queueQueryString } from '@/lib/constants';
import type { QueueParams } from '@/lib/types/grading';
import { useServerList } from '@/lib/utils/hooks';

import { buildColumns, isQueueStatus, PAGE_SIZE, STATUS_OPTIONS } from './const';

// ─── Giáo viên: hàng đợi chấm (UC-17, Prompt 6 A) ─────────────────────────────
// Bộ lọc nằm trên URL (?status&assignmentId&partId&classId&keyword&page): quay lại từ màn chấm vẫn đúng bộ lọc,
// và màn chấm đọc cùng bộ lọc để đi "Trước / Sau". Bài nộp trước xếp trước (server sắp).

type Option = { value: number; label: string };

const toNumber = (v: string | null) => (v && /^\d+$/.test(v) ? Number(v) : undefined);

export default function GradingQueuePage() {
  const router = useRouter();
  const pathname = usePathname();
  const sp = useSearchParams();
  const filter = useMemo<QueueParams>(
    () => ({
      status: isQueueStatus(sp.get('status')) ? (sp.get('status') as QueueParams['status']) : 'SUBMITTED',
      assignmentId: toNumber(sp.get('assignmentId')),
      partId: toNumber(sp.get('partId')),
      classId: toNumber(sp.get('classId')),
      keyword: sp.get('keyword') ?? undefined
    }),
    [sp]
  );
  const page = toNumber(sp.get('page')) ?? 0;
  const query = useMemo(() => ({ ...filter, page, size: PAGE_SIZE }), [filter, page]);
  const { data, total, loading } = useServerList(gradingApi.queue, query);

  const [assignments, setAssignments] = useState<Option[]>([]);
  const [classes, setClasses] = useState<Option[]>([]);
  const [parts, setParts] = useState<{ assignmentId: number; options: Option[] } | null>(null);

  // Danh sách chọn: bài tập đã phát hành và lớp của tôi (đủ dùng cho một học phần).
  useEffect(() => {
    let cancelled = false;
    assignmentApi
      .list({ page: 0, size: 100 })
      .then((res) => {
        if (cancelled) return;
        setAssignments(res.data.filter((a) => a.status !== 'DRAFT').map((a) => ({ value: a.id, label: a.title })));
      })
      .catch(() => undefined);
    classroomApi
      .listClasses({ page: 0, size: 100 })
      .then((res) => {
        if (!cancelled) setClasses(res.data.map((c) => ({ value: c.id, label: `${c.code} · ${c.name}` })));
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const assignmentId = filter.assignmentId;
    if (!assignmentId) return;
    let cancelled = false;
    assignmentApi
      .detail(assignmentId)
      .then((d) => {
        if (!cancelled) setParts({ assignmentId, options: d.parts.map((p) => ({ value: p.id, label: p.title })) });
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [filter.assignmentId]);

  const update = (patch: Partial<QueueParams> & { page?: number }) => {
    const next = { ...filter, page: 0, ...patch };
    const qs = queueQueryString({ ...next, status: next.status === 'SUBMITTED' ? undefined : next.status });
    const withPage = next.page ? `${qs ? `${qs}&` : ''}page=${next.page}` : qs;
    router.replace(withPage ? `${pathname}?${withPage}` : pathname, { scroll: false });
  };

  const columns = useMemo(() => buildColumns(filter), [filter]);
  const partOptions = parts && parts.assignmentId === filter.assignmentId ? parts.options : [];
  const filtered = Boolean(filter.assignmentId || filter.classId || filter.keyword);

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader title="Chấm bài" description="Bài nộp trước xếp trước. Chọn bài tập, phần hoặc lớp để lọc." />
      <Card styles={{ body: { padding: 0 } }}>
        <div className="flex flex-col gap-3 border-b border-neutral-100 p-4">
          <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
            <Select
              allowClear
              showSearch={{ optionFilterProp: 'label' }}
              placeholder="Bài tập"
              options={assignments}
              value={filter.assignmentId}
              onChange={(v?: number) => update({ assignmentId: v, partId: undefined })}
            />
            <Select
              allowClear
              placeholder="Phần"
              disabled={!filter.assignmentId}
              options={partOptions}
              value={filter.partId}
              onChange={(v?: number) => update({ partId: v })}
            />
            <Select
              allowClear
              showSearch={{ optionFilterProp: 'label' }}
              placeholder="Lớp"
              options={classes}
              value={filter.classId}
              onChange={(v?: number) => update({ classId: v })}
            />
            <Input.Search
              allowClear
              defaultValue={filter.keyword}
              placeholder="Mã SV hoặc họ tên"
              onSearch={(v) => update({ keyword: v.trim() || undefined })}
            />
          </div>
          <Segmented
            className="self-start"
            options={STATUS_OPTIONS}
            value={filter.status}
            onChange={(v) => update({ status: v as QueueParams['status'] })}
          />
        </div>
        <Table
          rowKey="submissionId"
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 820 }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  filtered
                    ? 'Không có bài nộp nào khớp bộ lọc'
                    : filter.status === 'SUBMITTED'
                      ? 'Không còn bài nào chờ chấm'
                      : 'Chưa có bài nộp nào ở trạng thái này'
                }
              />
            )
          }}
          pagination={{
            current: page + 1,
            pageSize: PAGE_SIZE,
            total,
            showSizeChanger: false,
            hideOnSinglePage: true,
            onChange: (p) => update({ page: p - 1 })
          }}
        />
      </Card>
    </div>
  );
}
