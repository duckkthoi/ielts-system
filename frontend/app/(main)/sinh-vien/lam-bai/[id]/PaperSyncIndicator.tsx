import clsx from 'clsx';
import { AlertTriangle, Check, CloudOff, Loader2 } from 'lucide-react';

import type { SyncStatus } from '@/lib/paperSync';

// ─── Trạng thái lưu của tài liệu paper (cùng kiểu với AutosaveIndicator của bài luận) ─
// Mỗi thay đổi được gửi lên server sau ~0,3 giây; "Đã lưu" khi server đã nhận hết.

export function PaperSyncIndicator({ status, onRetry }: { status: SyncStatus; onRetry: () => void }) {
  const base = 'inline-flex items-center gap-1 text-caption whitespace-nowrap';
  switch (status) {
    case 'loading':
    case 'sending':
      return (
        <span className={clsx(base, 'text-neutral-500')}>
          <Loader2 size={12} className="animate-spin" /> {status === 'loading' ? 'Đang mở…' : 'Đang lưu…'}
        </span>
      );
    case 'offline':
      return (
        <span className={clsx(base, 'text-warning')} title="Đừng đóng trang cho tới khi có mạng lại">
          <CloudOff size={12} /> Mất kết nối – sẽ gửi khi có mạng
        </span>
      );
    case 'error':
      return (
        <button
          type="button"
          onClick={onRetry}
          className={clsx(base, 'cursor-pointer border-0 bg-transparent p-0 text-error hover:underline')}
        >
          <AlertTriangle size={12} /> Lỗi đồng bộ – mở lại
        </button>
      );
    default:
      return (
        <span className={clsx(base, 'text-neutral-500')}>
          <Check size={12} className="text-success" /> Đã lưu
        </span>
      );
  }
}
