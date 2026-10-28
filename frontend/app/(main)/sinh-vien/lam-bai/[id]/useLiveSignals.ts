'use client';

import { useCallback, useEffect, useRef } from 'react';

import { publishRealtime } from '@/lib/realtime';
import type { ExamEventType } from '@/lib/types/exam';
import { nextAway } from '@/lib/utils/exam';
import type { AwayKind } from '@/lib/utils/exam';
import { serverNow } from '@/lib/utils/serverClock';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

// ─── Tín hiệu gửi giáo viên trong lúc làm bài (docs/05 B.2) ───────────────────
// - Xem trực tiếp (BR-26): gửi toàn bộ ô soạn qua `/app/live/{partId}/content`, tối đa 1 lần / giây khi đang gõ;
//   (kết nối lại) gửi ngay bản hiện tại để giáo viên đang xem có bản mới nhất. Không lưu gì — tự lưu nháp vẫn đi REST.
// - Chế độ thi (BR-25): rời / quay lại trang hoặc cửa sổ và dán văn bản (chỉ số ký tự) qua
//   `/app/exam/{sessionId}/event`, gửi bù khi có mạng lại. Mất kết nối do server tự ghi.

const LIVE_INTERVAL_MS = 1_000;

export function useLiveContent(partId: number, content: string) {
  const connected = useRealtimeStore((s) => s.status) === 'CONNECTED';
  const contentRef = useRef(content);
  const lastSentRef = useRef<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const flush = useCallback(() => {
    timerRef.current = null;
    const text = contentRef.current;
    if (text === lastSentRef.current) return;
    if (publishRealtime(`/app/live/${partId}/content`, { content: text })) lastSentRef.current = text;
  }, [partId]);

  useEffect(() => {
    contentRef.current = content;
    if (!connected || content === lastSentRef.current || timerRef.current) return;
    timerRef.current = setTimeout(flush, LIVE_INTERVAL_MS);
  }, [content, connected, flush]);

  // Mới kết nối (vào màn, kết nối lại): gửi ngay.
  useEffect(() => {
    if (!connected) return;
    lastSentRef.current = null;
    flush();
  }, [connected, flush]);

  useEffect(
    () => () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    },
    []
  );
}

/** Chờ chừng này sau `blur` rồi mới xét: chuyển tab bắn `blur` trước `visibilitychange`, chờ để gộp thành một sự kiện. */
const BLUR_SETTLE_MS = 300;
/** Số sự kiện tối đa giữ trên máy khi mất mạng (server cũng giới hạn 30 sự kiện / phút / phiên). */
const MAX_QUEUED = 30;

interface QueuedEvent {
  type: ExamEventType;
  pasteChars?: number;
  occurredAt: string;
}

/**
 * Chế độ thi: báo rời trang (ẩn tab) / rời cửa sổ (Alt+Tab, màn hình 2) và quay lại, dán văn bản (BR-25).
 * Mỗi sự kiện kèm giờ xảy ra theo giờ server; gửi không được (mất mạng) thì giữ lại, kết nối lại thì gửi bù theo
 * thứ tự — server ghi đúng giờ xảy ra và gắn "gửi bù". Trả hàm báo dán; `sessionId = null` thì không làm gì.
 */
export function useExamSignals(sessionId: number | null) {
  const connected = useRealtimeStore((s) => s.status) === 'CONNECTED';
  const queueRef = useRef<QueuedEvent[]>([]);
  const awayRef = useRef<AwayKind>(null);

  const flush = useCallback(() => {
    if (sessionId === null) return;
    while (queueRef.current.length > 0) {
      if (!publishRealtime(`/app/exam/${sessionId}/event`, queueRef.current[0])) return;
      queueRef.current.shift();
    }
  }, [sessionId]);

  const send = useCallback(
    (type: ExamEventType, pasteChars?: number) => {
      if (sessionId === null) return;
      queueRef.current = [
        ...queueRef.current,
        { type, pasteChars, occurredAt: new Date(serverNow()).toISOString() }
      ].slice(-MAX_QUEUED);
      flush();
    },
    [sessionId, flush]
  );

  // Có mạng lại → gửi bù những gì đã giữ.
  useEffect(() => {
    if (connected) flush();
  }, [connected, flush]);

  useEffect(() => {
    if (sessionId === null) return;
    let timer: ReturnType<typeof setTimeout> | null = null;
    const evaluate = () => {
      timer = null;
      const next = nextAway(awayRef.current, document.visibilityState === 'hidden', document.hasFocus());
      awayRef.current = next.away;
      if (next.event) send(next.event);
    };
    const onBlur = () => {
      if (timer) clearTimeout(timer);
      timer = setTimeout(evaluate, BLUR_SETTLE_MS);
    };
    const onChange = () => {
      if (timer) clearTimeout(timer);
      evaluate();
    };
    document.addEventListener('visibilitychange', onChange);
    window.addEventListener('blur', onBlur);
    window.addEventListener('focus', onChange);
    return () => {
      if (timer) clearTimeout(timer);
      document.removeEventListener('visibilitychange', onChange);
      window.removeEventListener('blur', onBlur);
      window.removeEventListener('focus', onChange);
    };
  }, [sessionId, send]);

  return useCallback((chars: number) => send('PASTE', chars), [send]);
}
