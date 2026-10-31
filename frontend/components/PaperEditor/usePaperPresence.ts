'use client';

import { useEffect, useRef, useState } from 'react';

import { publishRealtime, subscribeRealtime } from '@/lib/realtime';
import {
  applyPresence,
  encodePresence,
  onlinePeers,
  PRESENCE_HEARTBEAT_MS,
  type PresencePeer
} from '@/lib/utils/paperPresence';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

// ─── Người đang mở tài liệu Paper (awareness, lib/utils/paperPresence.ts) ─────
// `announce`: sinh viên gửi nhịp kèm mục đang soạn; giáo viên chỉ nghe (server không cho giáo viên gửi).

export function usePaperPresence(documentId: number | null, options: { announce: boolean; section?: string | null }) {
  const { announce, section = null } = options;
  const [peers, setPeers] = useState<Record<number, PresencePeer>>({});
  const [now, setNow] = useState(() => Date.now());
  const connected = useRealtimeStore((s) => s.status) === 'CONNECTED';

  useEffect(() => {
    if (documentId === null) return;
    return subscribeRealtime(`/topic/paper/${documentId}/awareness`, (payload) => {
      const push = payload as { userId: number; userName: string; state: string };
      setPeers((cur) => applyPresence(cur, push, Date.now()));
    });
  }, [documentId]);

  const sectionRef = useRef(section);
  const live = documentId !== null && announce && connected;

  // Nhịp đều (gửi ngay khi kết nối / kết nối lại); rời màn → báo đã rời.
  useEffect(() => {
    if (!live) return;
    const destination = `/app/paper/${documentId}/awareness`;
    const beat = () => publishRealtime(destination, { state: encodePresence(true, sectionRef.current) });
    beat();
    const timer = setInterval(beat, PRESENCE_HEARTBEAT_MS);
    return () => {
      clearInterval(timer);
      publishRealtime(destination, { state: encodePresence(false, null) });
    };
  }, [live, documentId]);

  // Đổi mục → báo ngay, không chờ nhịp.
  useEffect(() => {
    if (sectionRef.current === section) return;
    sectionRef.current = section;
    if (live) publishRealtime(`/app/paper/${documentId}/awareness`, { state: encodePresence(true, section) });
  }, [live, documentId, section]);

  // Đồng hồ để loại người im quá lâu.
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), PRESENCE_HEARTBEAT_MS / 3);
    return () => clearInterval(timer);
  }, []);

  return onlinePeers(peers, now);
}
