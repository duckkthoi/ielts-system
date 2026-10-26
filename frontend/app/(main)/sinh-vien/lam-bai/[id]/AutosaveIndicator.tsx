import clsx from 'clsx';
import { AlertTriangle, Check, CloudOff, Loader2, PenLine } from 'lucide-react';

import { formatTime } from '@/lib/utils/date';

import type { SaveState } from './useDraftAutosave';

// ─── Trạng thái tự lưu cạnh ô soạn (docs/06 mục 7, docs/07 mục 7.2) ───────────
// Lỗi lưu thì bấm vào để thử lại.

export function AutosaveIndicator({
  state,
  savedAt,
  error,
  onRetry
}: {
  state: SaveState;
  savedAt: string | null;
  error: string | null;
  onRetry: () => void;
}) {
  const base = 'inline-flex items-center gap-1 text-caption whitespace-nowrap';
  switch (state) {
    case 'SAVING':
      return (
        <span className={clsx(base, 'text-neutral-500')}>
          <Loader2 size={12} className="animate-spin" /> Đang lưu…
        </span>
      );
    case 'DIRTY':
      return (
        <span className={clsx(base, 'text-neutral-500')}>
          <PenLine size={12} /> Chưa lưu
        </span>
      );
    case 'OFFLINE':
      return (
        <span className={clsx(base, 'text-warning')}>
          <CloudOff size={12} /> Ngoại tuyến – đã lưu trên máy
        </span>
      );
    case 'ERROR':
      return (
        <button
          type="button"
          onClick={onRetry}
          title={error ?? undefined}
          className={clsx(base, 'cursor-pointer border-0 bg-transparent p-0 text-error hover:underline')}
        >
          <AlertTriangle size={12} /> Lỗi lưu – thử lại
        </button>
      );
    case 'CONFLICT':
      return (
        <span className={clsx(base, 'text-warning')}>
          <AlertTriangle size={12} /> Cần chọn bản nháp
        </span>
      );
    default:
      return (
        <span className={clsx(base, 'text-neutral-500')}>
          <Check size={12} className="text-success" />{' '}
          {savedAt ? `Đã lưu lúc ${formatTime(savedAt)}` : 'Chưa có thay đổi'}
        </span>
      );
  }
}
