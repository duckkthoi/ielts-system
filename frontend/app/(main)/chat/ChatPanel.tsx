'use client';

import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';

import { App, Button, Result, Skeleton } from 'antd';
import { ArrowLeft, ExternalLink } from 'lucide-react';
import Link from 'next/link';

import { chatApi } from '@/lib/api/chat';
import { gradingPath } from '@/lib/constants';
import { publishRealtime, subscribeRealtime } from '@/lib/realtime';
import type { ChatEvent, ChatHistory, ChatReader } from '@/lib/types/chat';
import { mergeMessages, setLocalStatus, upsertReader } from '@/lib/utils/chat';
import type { ChatLine } from '@/lib/utils/chat';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

import { Composer } from './Composer';
import { PAGE_SIZE, parseChannelId, SEND_TIMEOUT_MS, TYPING_THROTTLE_MS, TYPING_VISIBLE_MS } from './const';
import { MessageList } from './MessageList';

// ─── Khung chat của một kênh (UC-29, SD-12) ───────────────────────────────────
// - Lịch sử qua REST (mới nhất trước, tải ngược bằng `beforeId`); tin mới qua `/topic/chat/{id}`.
// - Gửi: hiện ngay "Đang gửi…", server phát lại tin đã lưu (cùng `clientMsgId`) thì thành tin thật;
//   10 giây không thấy / mất kết nối → "Chưa gửi được · Gửi lại" (gửi lại cùng `clientMsgId`, server không lưu trùng).
// - Đang mở kênh và tab đang hiện → gửi mốc "đã đọc" của tin cuối.
// - Kết nối lại sau khi mất → tải lại trang tin mới nhất để bù tin bị lỡ.

const newClientMsgId = () =>
  globalThis.crypto?.randomUUID?.() ?? `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;

const NEAR_BOTTOM_PX = 120;

export function ChatPanel({
  channelId,
  title,
  myId,
  myName,
  isTeacher,
  onRead,
  onBack
}: {
  channelId: string;
  title: string | null;
  myId: number;
  myName: string;
  isTeacher: boolean;
  onRead: (channelId: string) => void;
  onBack: () => void;
}) {
  const { message } = App.useApp();
  const parsed = parseChannelId(channelId);
  const realtimeStatus = useRealtimeStore((s) => s.status);
  const connected = realtimeStatus === 'CONNECTED' || realtimeStatus === 'SYNCING';

  const [lines, setLines] = useState<ChatLine[] | null>(null);
  const [reads, setReads] = useState<ChatReader[]>([]);
  const [notFound, setNotFound] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [typing, setTyping] = useState<string | null>(null);

  const linesRef = useRef<ChatLine[] | null>(null);
  useEffect(() => {
    linesRef.current = lines;
  }, [lines]);
  const scrollRef = useRef<HTMLDivElement>(null);
  const nearBottom = useRef(true);
  const keepOffset = useRef<number | null>(null);
  const lastTypingSent = useRef(0);
  const lastReadSent = useRef(0);
  const typingTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const sendTimers = useRef(new Map<string, ReturnType<typeof setTimeout>>());

  // ─── Lịch sử ───
  const applyLatest = useCallback(
    (history: ChatHistory) => {
      setLines((prev) => mergeMessages(prev ?? [], history.messages, myId));
      setReads((prev) => history.reads.reduce(upsertReader, prev));
      setHasMore((prev) => prev || history.messages.length === PAGE_SIZE);
    },
    [myId]
  );

  // Trang đầu khi mở kênh (component được gắn `key` theo kênh).
  useEffect(() => {
    let cancelled = false;
    chatApi
      .messages(channelId, undefined, PAGE_SIZE)
      .then((history) => {
        if (!cancelled) applyLatest(history);
      })
      .catch(() => {
        if (!cancelled) setNotFound(true);
      });
    return () => {
      cancelled = true;
    };
  }, [channelId, applyLatest]);

  const loadOlder = async () => {
    const oldest = lines?.find((l) => l.id !== null)?.id;
    if (!oldest) return;
    setLoadingOlder(true);
    try {
      const history = await chatApi.messages(channelId, oldest, PAGE_SIZE);
      keepOffset.current = scrollRef.current ? scrollRef.current.scrollHeight - scrollRef.current.scrollTop : null;
      setLines((prev) => mergeMessages(prev ?? [], history.messages, myId));
      setHasMore(history.messages.length === PAGE_SIZE);
    } catch {
      message.error('Không tải được tin cũ hơn');
    } finally {
      setLoadingOlder(false);
    }
  };

  // ─── Realtime ───
  useEffect(() => {
    const off = subscribeRealtime(`/topic/chat/${channelId}`, (payload) => {
      const ev = payload as ChatEvent;
      if (ev.type === 'MESSAGE') {
        setLines((prev) => mergeMessages(prev ?? [], [ev], myId));
        if (ev.senderId === myId && ev.clientMsgId) {
          clearTimeout(sendTimers.current.get(ev.clientMsgId));
          sendTimers.current.delete(ev.clientMsgId);
        } else {
          setTyping(null);
        }
      } else if (ev.type === 'TYPING' && ev.userId !== myId) {
        setTyping(ev.userName);
        if (typingTimer.current) clearTimeout(typingTimer.current);
        typingTimer.current = setTimeout(() => setTyping(null), TYPING_VISIBLE_MS);
      } else if (ev.type === 'READ') {
        // Tên người đọc lấy từ tin họ đã gửi (nếu có); upsertReader giữ tên cũ nếu đã biết.
        const userName = linesRef.current?.find((l) => l.senderId === ev.userId)?.senderName ?? '';
        setReads((prev) => upsertReader(prev, { userId: ev.userId, userName, lastMessageId: ev.lastMessageId }));
      }
    });
    const offErrors = subscribeRealtime('/user/queue/errors', (payload) => {
      const err = payload as { errorMessage?: string };
      message.error(err.errorMessage ?? 'Không gửi được tin nhắn');
    });
    const timers = sendTimers.current;
    return () => {
      off();
      offErrors();
      timers.forEach(clearTimeout);
      if (typingTimer.current) clearTimeout(typingTimer.current);
    };
  }, [channelId, myId, message]);

  // Kết nối lại → bù tin bị lỡ.
  const lastStatus = useRef(realtimeStatus);
  useEffect(() => {
    const was = lastStatus.current;
    lastStatus.current = realtimeStatus;
    if (realtimeStatus !== 'CONNECTED' || was === 'CONNECTED' || was === 'CONNECTING') return;
    chatApi
      .messages(channelId, undefined, PAGE_SIZE)
      .then(applyLatest)
      .catch(() => {
        // giữ tin đang có; lần kết nối sau sẽ thử lại
      });
  }, [realtimeStatus, channelId, applyLatest]);

  // ─── Đã đọc ───
  const lastId = lines?.filter((l) => l.id !== null).at(-1)?.id ?? 0;
  useEffect(() => {
    if (!connected || lastId <= lastReadSent.current) return;
    const markRead = () => {
      if (document.visibilityState !== 'visible' || lastId <= lastReadSent.current) return;
      if (publishRealtime(`/app/chat/${channelId}/read`, { lastMessageId: lastId })) {
        lastReadSent.current = lastId;
        onRead(channelId);
      }
    };
    markRead();
    document.addEventListener('visibilitychange', markRead);
    return () => document.removeEventListener('visibilitychange', markRead);
  }, [connected, lastId, channelId, onRead]);

  // ─── Cuộn: giữ ở đáy khi có tin mới, giữ vị trí khi tải tin cũ ───
  useLayoutEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    if (keepOffset.current !== null) {
      el.scrollTop = el.scrollHeight - keepOffset.current;
      keepOffset.current = null;
    } else if (nearBottom.current) {
      el.scrollTop = el.scrollHeight;
    }
  }, [lines, typing]);

  // ─── Gửi ───
  const publish = (line: ChatLine) => {
    const clientMsgId = line.clientMsgId as string;
    const ok = publishRealtime(`/app/chat/${channelId}/send`, { content: line.content, clientMsgId });
    if (!ok) {
      setLines((prev) => setLocalStatus(prev ?? [], clientMsgId, 'failed'));
      return;
    }
    clearTimeout(sendTimers.current.get(clientMsgId));
    sendTimers.current.set(
      clientMsgId,
      setTimeout(() => setLines((prev) => setLocalStatus(prev ?? [], clientMsgId, 'failed')), SEND_TIMEOUT_MS)
    );
  };

  const send = (content: string) => {
    const clientMsgId = newClientMsgId();
    const line: ChatLine = {
      key: `c-${clientMsgId}`,
      id: null,
      senderId: myId,
      senderName: myName,
      content,
      clientMsgId,
      createdAt: new Date().toISOString(),
      local: 'sending'
    };
    nearBottom.current = true;
    setLines((prev) => [...(prev ?? []), line]);
    publish(line);
  };

  const retry = (line: ChatLine) => {
    setLines((prev) => setLocalStatus(prev ?? [], line.clientMsgId as string, 'sending'));
    publish({ ...line, local: 'sending' });
  };

  const sendTyping = () => {
    const now = Date.now();
    if (now - lastTypingSent.current < TYPING_THROTTLE_MS) return;
    lastTypingSent.current = now;
    publishRealtime(`/app/chat/${channelId}/typing`, {});
  };

  const subtitle = parsed?.type === 'CLASSROOM' ? 'Kênh chung của lớp' : 'Trao đổi về một bài nộp';

  return (
    <div className="flex h-full min-w-0 flex-col">
      <header className="flex items-center gap-2 border-b border-neutral-100 px-4 py-3">
        <Button
          type="text"
          className="md:hidden!"
          icon={<ArrowLeft size={16} />}
          onClick={onBack}
          aria-label="Danh sách"
        />
        <div className="min-w-0 flex-1">
          <p className="truncate font-semibold text-neutral-900">{title ?? 'Cuộc trò chuyện mới'}</p>
          <p className="truncate text-small text-neutral-500">
            {typing ? <span className="text-brand-600">{typing} đang gõ…</span> : subtitle}
          </p>
        </div>
        {isTeacher && parsed?.type === 'SUBMISSION' && (
          <Link href={gradingPath(parsed.refId)}>
            <Button type="link" icon={<ExternalLink size={14} />}>
              Mở bài nộp
            </Button>
          </Link>
        )}
      </header>

      <div
        ref={scrollRef}
        className="min-h-0 flex-1 overflow-y-auto px-4 py-3"
        onScroll={(e) => {
          const el = e.currentTarget;
          nearBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < NEAR_BOTTOM_PX;
        }}
      >
        {notFound ? (
          <Result
            status="404"
            title="Không mở được cuộc trò chuyện"
            subTitle="Kênh không tồn tại hoặc bạn không thuộc kênh này."
          />
        ) : !lines ? (
          <Skeleton active paragraph={{ rows: 6 }} />
        ) : (
          <>
            {hasMore && (
              <div className="mb-2 text-center">
                <Button size="small" loading={loadingOlder} onClick={loadOlder}>
                  Tải tin cũ hơn
                </Button>
              </div>
            )}
            {lines.length === 0 ? (
              <p className="mt-10 text-center text-neutral-500">Chưa có tin nhắn. Hãy gửi lời chào đầu tiên.</p>
            ) : (
              <MessageList
                lines={lines}
                reads={reads}
                myId={myId}
                showNames={parsed?.type === 'CLASSROOM'}
                onRetry={retry}
              />
            )}
          </>
        )}
      </div>

      {!notFound && <Composer connected={connected} onSend={send} onTyping={sendTyping} />}
    </div>
  );
}
