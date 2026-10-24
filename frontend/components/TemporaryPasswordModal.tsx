'use client';

import { useCallback } from 'react';

import { Alert, App, Button, Modal } from 'antd';
import { Copy } from 'lucide-react';

// ─── Hiện mật khẩu tạm đúng một lần (tạo giáo viên, đặt lại mật khẩu) ─────────
// Mật khẩu chỉ nằm trong state của trang, không lưu ở đâu khác (CLAUDE.md ràng buộc 4).

export interface TemporaryPasswordInfo {
  title: string;
  username: string;
  password: string;
}

export function TemporaryPasswordModal({ info, onClose }: { info: TemporaryPasswordInfo | null; onClose: () => void }) {
  const { message } = App.useApp();

  const handleCopy = useCallback(async () => {
    if (!info) return;
    try {
      await navigator.clipboard.writeText(info.password);
      message.success('Đã sao chép mật khẩu');
    } catch {
      message.error('Không sao chép được, hãy chép tay mật khẩu');
    }
  }, [info, message]);

  return (
    <Modal
      open={!!info}
      title={info?.title}
      onCancel={onClose}
      mask={{ closable: false }}
      footer={
        <Button type="primary" onClick={onClose}>
          Đã sao chép và lưu
        </Button>
      }
    >
      <Alert
        type="warning"
        showIcon
        title="Mật khẩu này chỉ hiển thị một lần. Hãy gửi cho người dùng; họ sẽ phải đổi mật khẩu khi đăng nhập."
      />
      <p className="mt-4 text-small text-neutral-500">
        Tên đăng nhập: <span className="font-mono text-neutral-900">{info?.username}</span>
      </p>
      <div className="mt-2 flex items-center justify-between gap-3 rounded-md border border-neutral-100 bg-neutral-25 px-4 py-3">
        <span className="font-mono text-h2 tracking-wide text-neutral-900 select-all">{info?.password}</span>
        <Button icon={<Copy size={16} />} onClick={handleCopy}>
          Sao chép
        </Button>
      </div>
    </Modal>
  );
}
