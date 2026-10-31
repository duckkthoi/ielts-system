'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

import { paperDocApi } from '@/lib/api/paperDoc';
import { PaperSync, type SyncStatus } from '@/lib/paperSync';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

// ─── Giáo viên xem trực tiếp tài liệu Paper của một nhóm (chỉ nhận, H4) ───────
// Nạp trạng thái đầy đủ rồi nghe `/topic/paper/{id}` như thành viên nhóm, nhưng `PaperSync` ở chế độ chỉ nhận —
// không bao giờ gửi update, nên giáo viên không sửa được nội dung của sinh viên. Kết nối lại → lấy bù.

export function useTeacherPaperDoc(documentId: number | null) {
  const [sync, setSync] = useState<PaperSync | null>(null);
  const [status, setStatus] = useState<SyncStatus>('loading');
  const [failed, setFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);
  /** Lúc nhận thay đổi gần nhất từ nhóm (trong phiên xem này). */
  const [lastChangeAt, setLastChangeAt] = useState<Date | null>(null);
  const connected = useRealtimeStore((s) => s.status) === 'CONNECTED';
  const wasConnected = useRef(connected);

  useEffect(() => {
    if (documentId === null) return;
    let cancelled = false;
    let opened: PaperSync | null = null;
    const onUpdate = () => setLastChangeAt(new Date());
    paperDocApi
      .state(documentId)
      .then((state) => {
        if (cancelled) return;
        const s = new PaperSync(documentId, setStatus, () => 0, true);
        opened = s;
        s.start(state);
        s.doc.on('update', onUpdate);
        setSync(s);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });
    return () => {
      cancelled = true;
      opened?.doc.off('update', onUpdate);
      opened?.destroy();
      setSync(null);
      setLastChangeAt(null);
    };
  }, [documentId, attempt]);

  useEffect(() => {
    if (connected && !wasConnected.current) void sync?.onReconnected();
    wasConnected.current = connected;
  }, [connected, sync]);

  const retry = useCallback(() => {
    setFailed(false);
    setStatus('loading');
    setAttempt((a) => a + 1);
  }, []);

  return { sync, status, failed, retry, lastChangeAt };
}
