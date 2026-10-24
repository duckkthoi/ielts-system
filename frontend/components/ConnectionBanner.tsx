'use client';

import { useEffect, useRef, useState } from 'react';

import { Alert } from 'antd';

import { useRealtimeStore } from '@/stores/useRealtimeStore';

// ─── Thanh báo mất kết nối dưới header (docs/06 mục 6) ────────────────────────
// RECONNECTING: thanh vàng "đang thử lại (lần n)". Kết nối lại được: thanh xanh "Đã kết nối lại" vài giây.

const RECONNECTED_VISIBLE_MS = 3_000;

export function ConnectionBanner() {
  const status = useRealtimeStore((s) => s.status);
  const attempt = useRealtimeStore((s) => s.attempt);
  const [showReconnected, setShowReconnected] = useState(false);
  const wasLost = useRef(false);

  useEffect(() => {
    if (status === 'RECONNECTING') {
      wasLost.current = true;
      return;
    }
    if (status === 'CONNECTED' && wasLost.current) {
      wasLost.current = false;
      setShowReconnected(true);
      const timer = setTimeout(() => setShowReconnected(false), RECONNECTED_VISIBLE_MS);
      return () => clearTimeout(timer);
    }
  }, [status]);

  if (status === 'RECONNECTING') {
    return <Alert banner type="warning" showIcon title={`Mất kết nối… đang thử lại (lần ${attempt})`} />;
  }
  if (showReconnected) return <Alert banner type="success" showIcon title="Đã kết nối lại" />;
  return null;
}
