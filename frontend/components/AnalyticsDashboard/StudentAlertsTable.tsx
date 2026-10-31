'use client';

import { useState } from 'react';

import { Button, Card, Segmented, Table, Tag } from 'antd';
import { Download } from 'lucide-react';

import { analyticsApi } from '@/lib/api/analytics';
import { formatBand } from '@/lib/theme/status';
import type { AnalyticsFilter, AnalyticsScope, AnalyticsStudentRow } from '@/lib/types/analytics';
import { formatRelative } from '@/lib/utils/date';
import { useServerList } from '@/lib/utils/hooks';

// ─── Bảng sinh viên cần chú ý (quá hạn, không hoạt động ≥ 7 ngày, sửa bài ≥ 2 lần) ─
// Phân trang phía server; "Tất cả" = mọi sinh viên trong phạm vi. Xuất CSV / XLSX theo đúng bộ lọc đang xem.

const PAGE_SIZE = 10;

type Query = { scope: AnalyticsScope } & AnalyticsFilter & { alertsOnly: boolean; page: number; size: number };
const fetchStudents = (q: Query) => analyticsApi.students(q.scope, q);

export function StudentAlertsTable({ scope, filter }: { scope: AnalyticsScope; filter: AnalyticsFilter }) {
  const [alertsOnly, setAlertsOnly] = useState(true);
  const [exporting, setExporting] = useState<'CSV' | 'XLSX' | null>(null);
  // Trang gắn với bộ lọc: đổi bộ lọc → về trang đầu.
  const key = JSON.stringify({ filter, alertsOnly });
  const [paging, setPaging] = useState({ key, page: 0 });
  const current = paging.key === key ? paging.page : 0;

  const { data, total, loading } = useServerList(fetchStudents, {
    scope,
    ...filter,
    alertsOnly,
    page: current,
    size: PAGE_SIZE
  });

  const exportFile = async (format: 'CSV' | 'XLSX') => {
    setExporting(format);
    try {
      await analyticsApi.exportStudents(scope, filter, format);
    } catch {
      // đã báo lỗi
    } finally {
      setExporting(null);
    }
  };

  return (
    <Card
      title="Sinh viên cần chú ý"
      styles={{ body: { padding: 0 } }}
      extra={
        <span className="flex flex-wrap gap-2">
          <Button
            size="small"
            icon={<Download size={14} />}
            loading={exporting === 'CSV'}
            onClick={() => exportFile('CSV')}
          >
            CSV
          </Button>
          <Button
            size="small"
            icon={<Download size={14} />}
            loading={exporting === 'XLSX'}
            onClick={() => exportFile('XLSX')}
          >
            Excel
          </Button>
        </span>
      }
    >
      <div className="border-b border-neutral-100 px-4 py-3">
        <Segmented<boolean>
          value={alertsOnly}
          onChange={setAlertsOnly}
          options={[
            { value: true, label: 'Có cảnh báo' },
            { value: false, label: 'Tất cả' }
          ]}
        />
      </div>
      <Table<AnalyticsStudentRow>
        rowKey="studentId"
        size="middle"
        loading={loading}
        dataSource={data}
        scroll={{ x: 760 }}
        locale={{ emptyText: alertsOnly ? 'Không có sinh viên nào cần chú ý' : 'Chưa có sinh viên trong phạm vi lọc' }}
        pagination={{
          current: current + 1,
          pageSize: PAGE_SIZE,
          total,
          showSizeChanger: false,
          showTotal: (n) => `${n} sinh viên`,
          onChange: (p) => setPaging({ key, page: p - 1 })
        }}
        columns={[
          {
            title: 'Sinh viên',
            key: 'student',
            render: (_, r) => (
              <span>
                <span className="block font-medium text-neutral-900">{r.fullName}</span>
                <span className="font-mono text-caption text-neutral-500">
                  {r.studentCode ?? '—'}
                  {r.classes ? ` · ${r.classes}` : ''}
                </span>
              </span>
            )
          },
          {
            title: 'Đã nộp / được giao',
            key: 'done',
            render: (_, r) => <span className="tabular-nums">{`${r.submitted}/${r.assigned}`}</span>
          },
          {
            title: 'Quá hạn',
            dataIndex: 'overdue',
            render: (v: number) => <span className={v > 0 ? 'text-error tabular-nums' : 'tabular-nums'}>{v}</span>
          },
          { title: 'Lần sửa', dataIndex: 'revisions', responsive: ['md'] },
          {
            title: 'Band gần nhất',
            key: 'band',
            responsive: ['md'],
            render: (_, r) =>
              r.latestBand === null ? (
                '—'
              ) : (
                <span className="tabular-nums">
                  {formatBand(r.latestBand)}
                  {r.progress !== null && r.progress !== 0 && (
                    <span className={r.progress > 0 ? 'text-success' : 'text-error'}>
                      {' '}
                      ({r.progress > 0 ? '+' : ''}
                      {r.progress})
                    </span>
                  )}
                </span>
              )
          },
          {
            title: 'Hoạt động cuối',
            dataIndex: 'lastActivityAt',
            responsive: ['lg'],
            render: (v: string | null) => (v ? formatRelative(v) : 'Chưa có')
          },
          {
            title: 'Cảnh báo',
            dataIndex: 'alerts',
            render: (alerts: string[]) =>
              alerts.length === 0 ? (
                <span className="text-neutral-500">—</span>
              ) : (
                <span className="flex flex-wrap gap-1">
                  {alerts.map((a) => (
                    <Tag key={a} color="orange" className="m-0">
                      {a}
                    </Tag>
                  ))}
                </span>
              )
          }
        ]}
      />
    </Card>
  );
}
