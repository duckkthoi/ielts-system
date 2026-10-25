'use client';

import { Alert, Button, Table } from 'antd';
import { Download } from 'lucide-react';

import type { ImportReport } from '@/lib/types/classroom';
import { formatTime } from '@/lib/utils/date';

// ─── Kết quả import (docs/07 mục 7.2 ImportResult) ────────────────────────────
// 4 số (tạo mới / đã có / đã trong lớp / lỗi) + bảng lỗi + nút tải file tài khoản (chỉ tải một lần).

const STATS: { key: 'created' | 'existing' | 'skipped' | 'errors'; label: string; className: string }[] = [
  { key: 'created', label: 'Tạo mới', className: 'border-success-border bg-success-bg text-success' },
  { key: 'existing', label: 'Đã có tài khoản', className: 'border-info-border bg-info-bg text-info' },
  { key: 'skipped', label: 'Đã trong lớp', className: 'border-neutral-100 bg-neutral-25 text-neutral-700' },
  { key: 'errors', label: 'Lỗi', className: 'border-error-border bg-error-bg text-error' }
];

interface ImportResultProps {
  report: ImportReport;
  downloaded: boolean;
  downloading: boolean;
  onDownload: () => void;
}

export function ImportResult({ report, downloaded, downloading, onDownload }: ImportResultProps) {
  const ok = report.totalRows - report.errors.length;
  const values = { ...report, errors: report.errors.length };

  return (
    <div className="flex flex-col gap-4">
      <Alert
        type={report.errors.length === 0 ? 'success' : 'warning'}
        showIcon
        title="Import hoàn tất"
        description={`${ok}/${report.totalRows} dòng đã được xử lý thành công.`}
      />
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {STATS.map((stat) => (
          <div key={stat.key} className={`rounded-lg border px-4 py-3 ${stat.className}`}>
            <p className="text-caption font-medium">{stat.label}</p>
            <p className="text-h1 font-semibold tabular-nums">{values[stat.key]}</p>
          </div>
        ))}
      </div>
      {report.errors.length > 0 && (
        <Table
          size="small"
          rowKey={(row) => `${row.row}-${row.studentCode ?? ''}`}
          dataSource={report.errors}
          pagination={report.errors.length > 8 ? { pageSize: 8, size: 'small', showSizeChanger: false } : false}
          columns={[
            { title: 'Dòng', dataIndex: 'row', width: 70 },
            {
              title: 'Mã SV',
              dataIndex: 'studentCode',
              width: 130,
              render: (value: string | null) => <span className="font-mono">{value || '—'}</span>
            },
            { title: 'Lý do', dataIndex: 'reason' }
          ]}
        />
      )}
      {report.credentialsToken ? (
        <Alert
          type={downloaded ? 'success' : 'warning'}
          showIcon
          title={
            downloaded
              ? 'Đã tải file tài khoản. File chứa mật khẩu tạm — hãy lưu cẩn thận và chỉ gửi cho đúng sinh viên.'
              : `File tài khoản chứa mật khẩu tạm của ${report.created} sinh viên mới, chỉ tải được một lần` +
                (report.credentialsExpiresAt ? ` (trước ${formatTime(report.credentialsExpiresAt)}).` : '.')
          }
          action={
            !downloaded && (
              <Button type="primary" icon={<Download size={16} />} loading={downloading} onClick={onDownload}>
                Tải file tài khoản
              </Button>
            )
          }
        />
      ) : (
        <p className="text-small text-neutral-500">Không có tài khoản mới nên không có file tài khoản.</p>
      )}
      {report.emailQueued && report.created > 0 && (
        <p className="text-small text-neutral-500">
          Email thông tin đăng nhập đã được đưa vào hàng gửi cho các sinh viên mới có email.
        </p>
      )}
    </div>
  );
}
