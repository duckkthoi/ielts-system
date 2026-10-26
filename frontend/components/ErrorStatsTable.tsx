import { Table } from 'antd';

import { ERROR_GROUP_LABEL } from '@/lib/constants';
import type { ErrorStat } from '@/lib/types/stats';

// ─── Mã lỗi hay gặp: hồ sơ lỗi của sinh viên / lỗi hay gặp của một bài (docs/08 mục 4.5) ─
// Cột "Số lần" có thanh nhỏ so với mã nhiều nhất để nhìn nhanh mức độ.

export function ErrorStatsTable({ errors, showStudentCount }: { errors: ErrorStat[]; showStudentCount?: boolean }) {
  const max = Math.max(1, ...errors.map((e) => e.count));
  return (
    <Table<ErrorStat>
      size="small"
      rowKey="code"
      pagination={false}
      dataSource={errors}
      scroll={{ x: 'max-content' }}
      columns={[
        {
          title: 'Mã',
          dataIndex: 'code',
          width: 80,
          render: (code: string) => <span className="font-mono text-small font-semibold text-neutral-900">{code}</span>
        },
        {
          title: 'Lỗi',
          dataIndex: 'name',
          render: (name: string | null) => name ?? <span className="text-neutral-400">Mã riêng đã xoá</span>
        },
        {
          title: 'Nhóm',
          dataIndex: 'criterion',
          width: 90,
          render: (c: string | null) => (c ? (ERROR_GROUP_LABEL[c] ?? c) : '—')
        },
        {
          title: 'Số lần',
          dataIndex: 'count',
          width: 140,
          render: (count: number) => (
            <div className="flex items-center gap-2">
              <span className="w-7 text-right tabular-nums">{count}</span>
              <span className="h-1.5 flex-1 rounded-full bg-neutral-50">
                <span className="block h-full rounded-full bg-brand-600" style={{ width: `${(count / max) * 100}%` }} />
              </span>
            </div>
          )
        },
        ...(showStudentCount
          ? [{ title: 'Số SV', dataIndex: 'studentCount', width: 70, align: 'right' as const }]
          : [])
      ]}
    />
  );
}
