'use client';

import { useCallback, useState } from 'react';

import { Alert, App, Button, Checkbox, Modal, Progress, Upload } from 'antd';
import { Download, FileSpreadsheet, UploadCloud } from 'lucide-react';

import { classroomApi } from '@/lib/api/classroom';
import { getErrorMessage } from '@/lib/apiError';
import type { ImportReport } from '@/lib/types/classroom';

import { IMPORT_ACCEPT, IMPORT_MAX_BYTES } from './const';
import { ImportResult } from './ImportResult';

// ─── Hộp thoại import sinh viên: chọn file → đang xử lý → kết quả (UC-11, SD-03) ─
// Không đọc / không log nội dung file ở client (CLAUDE.md ràng buộc 4) — chỉ gửi lên server.

type Step = 'upload' | 'processing' | 'result';

const formatSize = (bytes: number) =>
  bytes < 1024
    ? `${bytes} B`
    : bytes < 1024 * 1024
      ? `${Math.round(bytes / 1024)} KB`
      : `${(bytes / 1024 / 1024).toFixed(1)} MB`;

interface ImportModalProps {
  classId: number;
  open: boolean;
  onClose: () => void;
  /** Import xong (có thay đổi thành viên) → trang tải lại danh sách. */
  onImported: () => void;
}

export function ImportModal({ classId, open, onClose, onImported }: ImportModalProps) {
  const { modal, message } = App.useApp();
  const [step, setStep] = useState<Step>('upload');
  const [file, setFile] = useState<File | null>(null);
  const [sendEmail, setSendEmail] = useState(true);
  const [uploadPercent, setUploadPercent] = useState(0);
  const [fileError, setFileError] = useState<string | null>(null);
  const [report, setReport] = useState<ImportReport | null>(null);
  const [downloaded, setDownloaded] = useState(false);
  const [downloading, setDownloading] = useState(false);

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

  // Có tài khoản mới mà chưa tải file → mất mật khẩu tạm, phải đặt lại từng người: hỏi lại trước khi đóng.
  const handleCancel = useCallback(() => {
    if (step === 'processing') return;
    if (step === 'result' && report?.credentialsToken && !downloaded) {
      modal.confirm({
        title: 'Bạn chưa tải file tài khoản',
        content: 'Nếu đóng, sẽ không lấy lại được mật khẩu tạm; phải đặt lại mật khẩu cho từng sinh viên mới.',
        okText: 'Vẫn đóng',
        okButtonProps: { danger: true },
        cancelText: 'Ở lại',
        onOk: close
      });
      return;
    }
    close();
  }, [step, report, downloaded, modal, close]);

  const handleImport = useCallback(async () => {
    if (!file) return;
    setStep('processing');
    setFileError(null);
    setUploadPercent(0);
    try {
      const result = await classroomApi.importStudents(classId, file, sendEmail, (event) => {
        if (event.total) setUploadPercent(Math.round((event.loaded / event.total) * 100));
      });
      setReport(result);
      setStep('result');
      onImported();
    } catch (error) {
      // Lỗi cả file (BAD_FILE: sai đuôi, thiếu cột, quá 2000 dòng…) → quay lại bước chọn file, hiện lý do.
      setFileError(getErrorMessage(error, 'Import thất bại'));
      setStep('upload');
    }
  }, [file, classId, sendEmail, onImported]);

  const handleDownload = useCallback(async () => {
    if (!report?.credentialsToken) return;
    setDownloading(true);
    try {
      await classroomApi.downloadCredentials(report.credentialsToken);
      setDownloaded(true);
    } catch {
      // axiosClient đã hiển thị lỗi (đã tải rồi / hết hạn)
    } finally {
      setDownloading(false);
    }
  }, [report]);

  const handleTemplate = useCallback(
    (format: 'xlsx' | 'csv') => {
      classroomApi.downloadTemplate(format).catch(() => undefined);
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
        <Button onClick={reset}>Import file khác</Button>
        <Button type="primary" onClick={handleCancel}>
          Xong
        </Button>
      </div>
    ) : null;

  return (
    <Modal
      title="Import sinh viên"
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
            accept={IMPORT_ACCEPT}
            maxCount={1}
            showUploadList={false}
            beforeUpload={(picked) => {
              if (picked.size > IMPORT_MAX_BYTES) {
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
                  <p className="text-small text-neutral-500">
                    {formatSize(file.size)} · bấm hoặc kéo file khác vào để đổi
                  </p>
                </>
              ) : (
                <>
                  <UploadCloud size={36} className="text-brand-600" />
                  <p className="text-neutral-900">Kéo thả file .xlsx hoặc .csv vào đây, hoặc bấm để chọn file</p>
                  <p className="text-small text-neutral-500">
                    Cột bắt buộc: <span className="font-mono">ma_sv</span>, <span className="font-mono">ho_ten</span>.
                    Tuỳ chọn: <span className="font-mono">email</span>. Tối đa 2000 dòng, 10 MB.
                  </p>
                </>
              )}
            </div>
          </Upload.Dragger>
          <div className="flex flex-wrap items-center justify-between gap-2">
            <span className="flex flex-wrap items-center gap-1 text-small text-neutral-500">
              <Download size={14} /> Tải file mẫu:
              <Button type="link" size="small" className="px-1" onClick={() => handleTemplate('xlsx')}>
                Excel
              </Button>
              <Button type="link" size="small" className="px-1" onClick={() => handleTemplate('csv')}>
                CSV
              </Button>
            </span>
            <Checkbox checked={sendEmail} onChange={(e) => setSendEmail(e.target.checked)}>
              Gửi thông tin đăng nhập qua email cho sinh viên mới
            </Checkbox>
          </div>
          <p className="text-small text-neutral-500">
            Sinh viên đã có tài khoản chỉ được thêm vào lớp (không đổi họ tên, email). Dòng lỗi được bỏ qua, các dòng
            khác vẫn được xử lý.
          </p>
        </div>
      )}

      {step === 'processing' && (
        <div className="flex flex-col items-center gap-3 py-8 text-center">
          <span className="grid size-13 place-items-center rounded-[14px] bg-brand-50 text-brand-600">
            <FileSpreadsheet size={24} />
          </span>
          <p className="text-h3 font-semibold text-neutral-900">Đang import danh sách sinh viên</p>
          <p className="text-small text-neutral-500">{file?.name}</p>
          <Progress className="w-full max-w-md" percent={uploadPercent < 100 ? uploadPercent : 100} status="active" />
          <p className="text-neutral-700">
            {uploadPercent < 100 ? 'Đang tải file lên…' : 'Đang kiểm tra từng dòng và tạo tài khoản…'}
          </p>
        </div>
      )}

      {step === 'result' && report && (
        <ImportResult report={report} downloaded={downloaded} downloading={downloading} onDownload={handleDownload} />
      )}
    </Modal>
  );
}
