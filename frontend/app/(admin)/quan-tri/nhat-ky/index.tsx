'use client';

import { useState } from 'react';

import { Card, DatePicker, Input, Select, Table } from 'antd';
import type { Dayjs } from 'dayjs';

import { PageHeader } from '@/components/PageHeader';
import { adminApi } from '@/lib/api/admin';
import type { AuditAction, AuditLog, AuditLogParams } from '@/lib/types/admin';
import { useServerList } from '@/lib/utils/hooks';

import { ACTION_OPTIONS, columns, PAGE_SIZE } from './const';

// ─── Quản trị: nhật ký hệ thống (UC-07) ───────────────────────────────────────
// Lọc theo hành động, tiền tố IP, khoảng ngày; mở dòng để xem chi tiết (JSON do server ghi).

export default function AuditLogPage() {
  const [query, setQuery] = useState<AuditLogParams>({ page: 0, size: PAGE_SIZE });
  const { data, total, loading } = useServerList(adminApi.listAuditLogs, query);

  // Khoảng ngày theo giờ máy: từ 00:00 ngày đầu tới hết ngày cuối (server lọc from ≤ t < to).
  const handleRange = (range: [Dayjs | null, Dayjs | null] | null) =>
    setQuery((q) => ({
      ...q,
      from: range?.[0]?.startOf('day').toISOString(),
      to: range?.[1]?.add(1, 'day').startOf('day').toISOString(),
      page: 0
    }));

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader
        title="Nhật ký hệ thống"
        description="Các thao tác quan trọng: đăng nhập, quản lý tài khoản, import, phát hành và trả bài — kèm IP."
      />
      <Card styles={{ body: { padding: 0 } }}>
        <div className="flex flex-wrap gap-2 border-b border-neutral-100 p-4">
          <Select
            allowClear
            showSearch={{ optionFilterProp: 'label' }}
            placeholder="Hành động"
            className="w-48"
            options={ACTION_OPTIONS}
            onChange={(action?: AuditAction) => setQuery((q) => ({ ...q, action, page: 0 }))}
          />
          <Input.Search
            allowClear
            placeholder="IP (vd 192.168.)"
            className="w-48"
            onSearch={(ip) => setQuery((q) => ({ ...q, ip: ip.trim() || undefined, page: 0 }))}
          />
          <DatePicker.RangePicker
            format="DD/MM/YYYY"
            placeholder={['Từ ngày', 'Đến ngày']}
            onChange={(range) => handleRange(range)}
          />
        </div>
        <Table<AuditLog>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 640 }}
          expandable={{
            rowExpandable: (row) => !!row.detail && Object.keys(row.detail).length > 0,
            expandedRowRender: (row) => (
              <pre className="m-0 overflow-x-auto rounded-md bg-neutral-25 p-3 font-mono text-caption text-neutral-700">
                {JSON.stringify(row.detail, null, 2)}
              </pre>
            )
          }}
          locale={{ emptyText: 'Không có dòng nhật ký nào khớp bộ lọc' }}
          pagination={{
            current: (query.page ?? 0) + 1,
            pageSize: PAGE_SIZE,
            total,
            showSizeChanger: false,
            showTotal: (n) => `${n} dòng`,
            onChange: (page) => setQuery((q) => ({ ...q, page: page - 1 }))
          }}
        />
      </Card>
    </div>
  );
}
