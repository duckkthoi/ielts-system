'use client';

import clsx from 'clsx';

import { useRealtimeStore } from '@/stores/useRealtimeStore';
import type { ConnectionStatus as Status } from '@/stores/useRealtimeStore';

// ─── Chấm trạng thái kết nối realtime trên header (docs/06 mục 6) ─────────────

const META: Record<Status, { label: string; dot: string; text: string }> = {
  CONNECTED: { label: 'Trực tuyến', dot: 'bg-presence-online', text: 'text-success' },
  SYNCING: { label: 'Đang đồng bộ…', dot: 'bg-presence-online', text: 'text-success' },
  CONNECTING: { label: 'Đang kết nối…', dot: 'bg-presence-offline', text: 'text-neutral-500' },
  RECONNECTING: { label: 'Mất kết nối', dot: 'bg-presence-away', text: 'text-warning' },
  DISCONNECTED: { label: 'Ngoại tuyến', dot: 'bg-presence-offline', text: 'text-neutral-500' }
};

export function ConnectionStatus() {
  const status = useRealtimeStore((s) => s.status);
  const meta = META[status];
  return (
    <span className={clsx('hidden items-center gap-1.5 text-caption md:flex', meta.text)} role="status">
      <span className={clsx('size-2 rounded-full ring-2 ring-neutral-0', meta.dot)} />
      {meta.label}
    </span>
  );
}
