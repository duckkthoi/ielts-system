'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

import { paperLiveCommentApi } from '@/lib/api/paperLiveComment';
import { subscribeRealtime } from '@/lib/realtime';
import type { LiveComment, LiveCommentPush } from '@/lib/types/paper';
import { applyCommentPush } from '@/lib/utils/liveComments';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

// ─── Nhận xét trực tiếp của một tài liệu Paper (H4) ───────────────────────────
// Tải danh sách bằng REST rồi nghe `/topic/paper/{id}/comments`; kết nối lại thì tải lại để không lỡ sự kiện.
// `onPush`: báo sự kiện mới (vd sinh viên hiện thông báo nhỏ — không lấy focus khỏi trình soạn).

export function useLiveComments(documentId: number | null, onPush?: (push: LiveCommentPush) => void) {
  const [comments, setComments] = useState<LiveComment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const connected = useRealtimeStore((s) => s.status) === 'CONNECTED';
  const onPushRef = useRef(onPush);
  useEffect(() => {
    onPushRef.current = onPush;
  });

  useEffect(() => {
    if (documentId === null) return;
    let cancelled = false;
    const unsubscribe = subscribeRealtime(`/topic/paper/${documentId}/comments`, (payload) => {
      const push = payload as LiveCommentPush;
      setComments((cur) => applyCommentPush(cur, push));
      onPushRef.current?.(push);
    });
    paperLiveCommentApi
      .list(documentId)
      .then((list) => {
        if (cancelled) return;
        setComments(list);
        setError(false);
      })
      .catch(() => {
        if (!cancelled) setError(true);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [documentId, attempt, connected]);

  const retry = useCallback(() => {
    setLoading(true);
    setAttempt((a) => a + 1);
  }, []);

  /** Cập nhật ngay theo response REST (không chờ sự kiện realtime). */
  const upsert = useCallback((c: LiveComment) => {
    setComments((cur) => applyCommentPush(cur, { type: 'UPDATED', comment: c, commentId: c.id }));
  }, []);
  const drop = useCallback((id: number) => {
    setComments((cur) => applyCommentPush(cur, { type: 'DELETED', comment: null, commentId: id }));
  }, []);

  return { comments, loading, error, retry, upsert, drop };
}
