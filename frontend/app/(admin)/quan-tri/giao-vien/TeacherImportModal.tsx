'use client';

import { useCallback, useState } from 'react';

import { Alert, App, Button, Modal, Progress, Table, Upload } from 'antd';
import { Download, FileSpreadsheet, UploadCloud } from 'lucide-react';

import { adminApi } from '@/lib/api/admin';
import { getErrorMessage } from '@/lib/apiError';
import type { TeacherImportReport } from '@/lib/types/admin';
import { saveBlob, toCsv } from '@/lib/utils/download';

// ─── Hộp thoại import giáo viên: chọn file → đang xử lý → kết quả (UC-05b) ────
// Không đọc / không log nội dung file ở client (CLAUDE.md ràng buộc 4) — chỉ gửi lên server.
// Mật khẩu tạm chỉ có trong response: tải danh sách tài khoản (CSV tạo ngay trên máy) trước khi đóng.

type Step = 'upload' | 'processing' | 'result';

const ACCEPT = '.xlsx,.csv';
const MAX_BYTES = 10 * 1024 * 1024;

const STATS: { key: 'created' | 'skipped' | 'errors'; label: string; className: string }[] = [
  { key: 'created', label: 'Tạo mới', className: 'border-success-border bg-success-bg text-success' },
  { key: 'skipped', label: 'Đã là giáo viên', className: 'border-neutral-100 bg-neutral-25 text-neutral-700' },
  { key: 'errors', label: 'Lỗi', className: 'border-error-border bg-error-bg text-error' }
];

function ImportReportView({
  report,
  downloaded,
  onDownload
}: {
  report: TeacherImportReport;
  downloaded: boolean;
  onDownload: () => void;
}) {
  const values = { created: report.created, skipped: report.skipped, errors: report.errors.length };
  return (
    <div className="flex flex-col gap-4">
      <Alert
        type={report.errors.length === 0 ? 'success' : 'warning'}
        showIcon
        title="Import hoàn tất"
        description={`${report.totalRows - report.errors.length}/${report.totalRows} dòng đã được xử lý thành công.`}
      />
      <div className="grid grid-cols-3 gap-3">
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
          rowKey={(row) => `${row.row}-${row.username ?? ''}`}
          dataSource={report.errors}
          pagination={report.errors.length > 6 ? { pageSize: 6, size: 'small', showSizeChanger: false } : false}
          columns={[
            { title: 'Dòng', dataIndex: 'row', width: 70 },
            {
              title: 'Tên đăng nhập',
              dataIndex: 'username',
              width: 160,
              render: (value: string | null) => <span className="font-mono">{value || '—'}</span>
            },
            { title: 'Lý do', dataIndex: 'reason' }
          ]}
        />
      )}
      {report.accounts.length > 0 ? (
        <>
          <Alert
            type={downloaded ? 'success' : 'warning'}
            showIcon
            title={
              downloaded
                ? 'Đã tải danh sách tài khoản. File chứa mật khẩu tạm — lưu cẩn thận và chỉ gửi cho đúng người.'
                : `Mật khẩu tạm của ${report.accounts.length} giáo viên mới chỉ hiện một lần. Hãy tải danh sách trước khi đóng.`
            }
            action={
              <Button type={downloaded ? 'default' : 'primary'} icon={<Download size={16} />} onClick={onDownload}>
                {downloaded ? 'Tải lại' : 'Tải danh sách tài khoản'}
              </Button>
            }
          />
          <Table
            size="small"
            rowKey="id"
            dataSource={report.accounts}
            pagination={report.accounts.length > 6 ? { pageSize: 6, size: 'small', showSizeChanger: false } : false}
            scroll={{ x: 520 }}
            columns={[
              {
                title: 'Tên đăng nhập',
                dataIndex: 'username',
                render: (v: string) => <span className="font-mono">{v}</span>
              },
              { title: 'Họ tên', dataIndex: 'fullName' },
              {
                title: 'Mật khẩu tạm',
                dataIndex: 'temporaryPassword',
                render: (v: string) => (
                  <span className="font-mono" style={{ userSelect: 'all' }}>
                    {v}
                  </span>
                )
              }
            ]}
          />
          <p className="text-small text-neutral-500">Giáo viên mới phải đổi mật khẩu ở lần đăng nhập đầu tiên.</p>
        </>
      ) : (
        <p className="text-small text-neutral-500">Không có tài khoản mới.</p>
      )}
    </div>
  );
}

export function TeacherImportModal({
  open,
  onClose,
  onImported
}: {
  open: boolean;
  onClose: () => void;
  /** Có giáo viên mới → trang tải lại danh sách. */
  onImported: () => void;
}) {
  const { modal, message } = App.useApp();
  const [step, setStep] = useState<Step>('upload');
  const [file, setFile] = useState<File | null>(null);
  const [uploadPercent, setUploadPercent] = useState(0);
  const [fileError, setFileError] = useState<string | null>(null);
  const [report, setReport] = useState<TeacherImportReport | null>(null);
  const [downloaded, setDownloaded] = useState(false);

  const reset = useCallback(() => {
    setStep('upload');
    setFile(null);
    setUploadPercent(0);
    setFileError(null);
    setReport(null);
    setDownloaded(false);
  }, []);

  const close = useCallback(() => {
    onClose();
    reset();
  }, [onClose, reset]);

  // Có tài khoản mới mà chưa tải danh sách → mất mật khẩu tạm, phải đặt lại từng người: hỏi lại trước khi đóng.
  const confirmLeave = useCallback(
    (then: () => void) => {
      if (step === 'result' && report && report.accounts.length > 0 && !downloaded) {
        modal.confirm({
          title: 'Bạn chưa tải danh sách tài khoản',
          content: 'Nếu tiếp tục, sẽ không xem lại được mật khẩu tạm; phải đặt lại mật khẩu cho từng giáo viên mới.',
          okText: 'Vẫn tiếp tục',
          okButtonProps: { danger: true },
          cancelText: 'Ở lại',
          onOk: then
        });
        return;
      }
      then();
    },
    [step, report, downloaded, modal]
  );

  const handleCancel = useCallback(() => {
    if (step === 'processing') return;
    confirmLeave(close);
  }, [step, confirmLeave, close]);

  const handleImport = useCallback(async () => {
    if (!file) return;
    setStep('processing');
    setFileError(null);
    setUploadPercent(0);
    try {
      const result = await adminApi.importTeachers(file, (event) => {
        if (event.total) setUploadPercent(Math.round((event.loaded / event.total) * 100));
      });
      setReport(result);
      setStep('result');
      if (result.created > 0) onImported();
    } catch (error) {
      // Lỗi cả file (BAD_FILE: sai đuôi, thiếu cột, quá 500 dòng…) → quay lại bước chọn file, hiện lý do.
      setFileError(getErrorMessage(error, 'Import thất bại'));
      setStep('upload');
    }
  }, [file, onImported]);

  const handleDownload = useCallback(() => {
    if (!report) return;
    const csv = toCsv([
      ['STT', 'Tên đăng nhập', 'Họ tên', 'Email', 'Mật khẩu tạm'],
      ...report.accounts.map((a, i) => [i + 1, a.username, a.fullName, a.email, a.temporaryPassword])
    ]);
    saveBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), 'tai-khoan-giao-vien.csv');
    setDownloaded(true);
  }, [report]);

  const handleTemplate = useCallback(
    (format: 'xlsx' | 'csv') => {
      adminApi.downloadTeacherTemplate(format).catch(() => undefined);
      message.info('Đang tải file mẫu…');
    },
    [message]
  );

  const footer =
    step === 'upload' ? (
      <div className="flex justify-end gap-2">
        <Button onClick={handleCancel}>Huỷ</Button>
        <Button type="primary" icon={<UploadCloud size={16} />} disabled={!file} onClick={handleImport}>
          Import
        </Button>
      </div>
    ) : step === 'result' ? (
      <div className="flex justify-end gap-2">
        <Button onClick={() => confirmLeave(reset)}>Import file khác</Button>
        <Button type="primary" onClick={handleCancel}>
          Xong
        </Button>
      </div>
    ) : null;

  return (
    <Modal
      title="Import giáo viên"
      width={720}
      open={open}
      onCancel={handleCancel}
      mask={{ closable: false }}
      closable={step !== 'processing'}
      keyboard={step !== 'processing'}
      footer={footer}
      destroyOnHidden
    >
      {step === 'upload' && (
        <div className="flex flex-col gap-4">
          {fileError && <Alert type="error" showIcon title="Không import được file" description={fileError} />}
          <Upload.Dragger
            accept={ACCEPT}
            maxCount={1}
            showUploadList={false}
            beforeUpload={(picked) => {
              if (picked.size > MAX_BYTES) {
                setFileError('File vượt quá 10 MB.');
                return Upload.LIST_IGNORE;
              }
              setFile(picked);
              setFileError(null);
              return false;
            }}
          >
            <div className="flex flex-col items-center gap-2 px-4 py-2">
              {file ? (
                <>
                  <FileSpreadsheet size={36} className="text-success" />
                  <p className="font-medium text-neutral-900">{file.name}</p>
                  <p className="text-small text-neutral-500">Bấm hoặc kéo file khác vào để đổi</p>
                </>
              ) : (
                <>
                  <UploadCloud size={36} className="text-brand-600" />
                  <p className="text-neutral-900">Kéo thả file .xlsx hoặc .csv vào đây, hoặc bấm để chọn file</p>
                  <p className="text-small text-neutral-500">
                    Cột bắt buộc: <span className="font-mono">ten_dang_nhap</span>,{' '}
                    <span className="font-mono">ho_ten</span>. Tuỳ chọn: <span className="font-mono">email</span>. Tối
                    đa 500 dòng, 10 MB.
                  </p>
                </>
              )}
            </div>
          </Upload.Dragger>
          <span className="flex flex-wrap items-center gap-1 text-small text-neutral-500">
            <Download size={14} /> Tải file mẫu:
            <Button type="link" size="small" className="px-1" onClick={() => handleTemplate('xlsx')}>
              Excel
            </Button>
            <Button type="link" size="small" className="px-1" onClick={() => handleTemplate('csv')}>
              CSV
            </Button>
          </span>
          <p className="text-small text-neutral-500">
            Tên đăng nhập đã là giáo viên sẽ được bỏ qua (không đổi họ tên, email). Dòng lỗi được bỏ qua, các dòng khác
            vẫn được xử lý. Tài khoản mới nhận mật khẩu tạm và phải đổi ở lần đăng nhập đầu.
          </p>
        </div>
      )}

      {step === 'processing' && (
        <div className="flex flex-col items-center gap-3 py-8 text-center">
          <span className="grid size-13 place-items-center rounded-[14px] bg-brand-50 text-brand-600">
            <FileSpreadsheet size={24} />
          </span>
          <p className="text-h3 font-semibold text-neutral-900">Đang import danh sách giáo viên</p>
          <p className="text-small text-neutral-500">{file?.name}</p>
          <Progress className="w-full max-w-md" percent={uploadPercent < 100 ? uploadPercent : 100} status="active" />
          <p className="text-neutral-700">
            {uploadPercent < 100 ? 'Đang tải file lên…' : 'Đang kiểm tra từng dòng và tạo tài khoản…'}
          </p>
        </div>
      )}

      {step === 'result' && report && (
        <ImportReportView report={report} downloaded={downloaded} onDownload={handleDownload} />
      )}
    </Modal>
  );
}
