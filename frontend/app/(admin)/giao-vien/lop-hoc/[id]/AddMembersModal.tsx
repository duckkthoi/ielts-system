'use client';

import { useCallback, useState } from 'react';

import { Alert, Input, Modal } from 'antd';

import { classroomApi } from '@/lib/api/classroom';
import type { AddMembersResult } from '@/lib/types/classroom';

// ─── Thêm sinh viên đã có tài khoản bằng mã SV (mỗi dòng / dấu phẩy một mã) ───

/** Tách mã SV từ ô nhập: xuống dòng, dấu phẩy, chấm phẩy, khoảng trắng; bỏ trùng (không phân biệt hoa thường). */
function parseCodes(text: string): string[] {
  const seen = new Set<string>();
  return text
    .split(/[\s,;]+/)
    .map((code) => code.trim())
    .filter((code) => code && !seen.has(code.toUpperCase()) && seen.add(code.toUpperCase()));
}

interface AddMembersModalProps {
  classId: number;
  open: boolean;
  onClose: () => void;
  onAdded: () => void;
}

export function AddMembersModal({ classId, open, onClose, onAdded }: AddMembersModalProps) {
  const [text, setText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<AddMembersResult | null>(null);
  const codes = parseCodes(text);

  const handleClose = useCallback(() => {
    setText('');
    setResult(null);
    onClose();
  }, [onClose]);

  const handleOk = useCallback(async () => {
    if (result) {
      handleClose();
      return;
    }
    setSubmitting(true);
    try {
      const res = await classroomApi.addMembers(classId, codes);
      setResult(res);
      if (res.added > 0) onAdded();
    } catch {
      // axiosClient đã hiển thị toast lỗi
    } finally {
      setSubmitting(false);
    }
  }, [result, handleClose, classId, codes, onAdded]);

  return (
    <Modal
      title="Thêm sinh viên đã có tài khoản"
      open={open}
      onCancel={handleClose}
      onOk={handleOk}
      okText={result ? 'Xong' : `Thêm ${codes.length || ''} sinh viên`}
      okButtonProps={{ disabled: !result && codes.length === 0, loading: submitting }}
      cancelButtonProps={{ className: result ? 'hidden' : undefined }}
      cancelText="Huỷ"
      destroyOnHidden
    >
      {result ? (
        <div className="flex flex-col gap-3">
          <Alert
            type={result.notFound.length ? 'warning' : 'success'}
            showIcon
            title={`Đã thêm ${result.added} sinh viên · ${result.alreadyMember} đã có trong lớp`}
          />
          {result.notFound.length > 0 && (
            <div>
              <p className="mb-1 text-small text-neutral-500">
                Không tìm thấy tài khoản sinh viên ({result.notFound.length}) — dùng Import để tạo tài khoản mới:
              </p>
              <p className="font-mono text-small break-words text-error">{result.notFound.join(', ')}</p>
            </div>
          )}
        </div>
      ) : (
        <>
          <p className="mb-2 text-small text-neutral-500">
            Nhập mã sinh viên, mỗi dòng một mã (hoặc cách nhau bằng dấu phẩy). Sinh viên chưa có tài khoản thì dùng
            Import.
          </p>
          <Input.TextArea
            rows={6}
            className="font-mono"
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder={'2151000001\n2151000002'}
          />
        </>
      )}
    </Modal>
  );
}
