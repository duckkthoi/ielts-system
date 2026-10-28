'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

import clsx from 'clsx';
import { MessageSquare } from 'lucide-react';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';

import { ChatPanel, parseChannelId } from '@/components/ChatPanel';
import { chatApi } from '@/lib/api/chat';
import { ROLE } from '@/lib/constants';
import { subscribeRealtime } from '@/lib/realtime';
import type { ChannelSummary, ChatEvent } from '@/lib/types/chat';
import { applyIncomingToChannels } from '@/lib/utils/chat';
import { useAuthStore } from '@/stores/useAuthStore';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

import { ChannelList } from './ChannelList';

// ─── Trao đổi (UC-29, FR-55, Prompt 8) ────────────────────────────────────────
// Kênh đang mở nằm trên URL (?kenh=submission-12): màn chấm mở thẳng kênh của bài nộp, kể cả khi
// kênh chưa có tin (chưa có trong danh sách). Nghe mọi kênh trong danh sách để cập nhật tin cuối /
// số chưa đọc; tin tới kênh chưa có trong danh sách → tải lại danh sách. Điện thoại: một cột.

export default function ChatPage() {
  const router = useRouter();
  const pathname = usePathname();
  const activeId = useSearchParams().get('kenh');
  const validActive = activeId && parseChannelId(activeId) ? activeId : null;
  const user = useAuthStore((s) => s.user);
  const realtimeStatus = useRealtimeStore((s) => s.status);

  const [channels, setChannels] = useState<ChannelSummary[] | null>(null);
  const [studentOptions, setStudentOptions] = useState<{ value: string; studentId: number; label: string }[]>([]);
  const [studentSearch, setStudentSearch] = useState('');
  const [version, setVersion] = useState(0);
  const reload = useCallback(() => setVersion((v) => v + 1), []);

  useEffect(() => {
    let cancelled = false;
    chatApi
      .channels()
      .then((list) => {
        if (!cancelled) setChannels(list);
      })
      .catch(() => {
        if (!cancelled) setChannels((prev) => prev ?? []);
      });
    return () => {
      cancelled = true;
    };
  }, [version]);

  // Kết nối lại → danh sách (số chưa đọc, tin cuối) có thể đã cũ.
  const lastStatus = useRef(realtimeStatus);
  useEffect(() => {
    const was = lastStatus.current;
    lastStatus.current = realtimeStatus;
    if (realtimeStatus === 'CONNECTED' && was !== 'CONNECTED' && was !== 'CONNECTING') reload();
  }, [realtimeStatus, reload]);

  // Nghe mọi kênh đang có trong danh sách (khoá theo danh sách id để không đăng ký lại mỗi tin).
  const myId = user?.id ?? 0;
  const channelKey = (channels ?? []).map((c) => c.channelId).join(',');
  const activeRef = useRef(validActive);
  useEffect(() => {
    activeRef.current = validActive;
  }, [validActive]);
  useEffect(() => {
    if (!channelKey) return;
    const offs = channelKey.split(',').map((id) =>
      subscribeRealtime(`/topic/chat/${id}`, (payload) => {
        const ev = payload as ChatEvent;
        if (ev.type !== 'MESSAGE') return;
        setChannels((prev) => {
          if (!prev) return prev;
          const next = applyIncomingToChannels(prev, ev, myId, activeRef.current);
          if (next === null) reload();
          return next ?? prev;
        });
      })
    );
    return () => offs.forEach((off) => off());
  }, [channelKey, myId, reload]);

  // Tin ở kênh bài nộp còn được gửi riêng qua /user/queue/chat: kênh chưa có trong danh sách (bên kia
  // vừa nhắn lần đầu) → tải lại danh sách. Kênh đã có thì topic của kênh đã lo.
  const channelsRef = useRef(channels);
  useEffect(() => {
    channelsRef.current = channels;
  }, [channels]);
  useEffect(
    () =>
      subscribeRealtime('/user/queue/chat', (payload) => {
        const ev = payload as ChatEvent;
        if (ev.type !== 'MESSAGE') return;
        const list = channelsRef.current;
        if (list && !list.some((c) => c.channelId === ev.channelId)) reload();
      }),
    [reload]
  );

  // Kênh đang mở mà chưa có trong danh sách: tin đầu tiên tới thì tải lại danh sách để có tiêu đề.
  useEffect(() => {
    if (!validActive || !channels || channels.some((c) => c.channelId === validActive)) return;
    return subscribeRealtime(`/topic/chat/${validActive}`, (payload) => {
      if ((payload as ChatEvent).type === 'MESSAGE') reload();
    });
  }, [validActive, channels, reload]);

  const select = (channelId: string) => router.replace(`${pathname}?kenh=${channelId}`, { scroll: false });

  useEffect(() => {
    const keyword = studentSearch.trim();
    if (keyword.length < 2 || user?.role !== ROLE.TEACHER) {
      return;
    }
    let cancelled = false;
    const timer = setTimeout(() => {
      chatApi
        .searchStudents(keyword)
        .then((students) => {
          if (!cancelled) {
            setStudentOptions(
              students.map((s) => ({
                studentId: s.id,
                value: s.studentCode,
                label: `${s.studentCode} · ${s.fullName}`
              }))
            );
          }
        })
        .catch(() => {
          if (!cancelled) setStudentOptions([]);
        });
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [studentSearch, user?.role]);

  const openStudent = (studentId: number) => {
    chatApi.openDirect(studentId).then((channel) => {
      setChannels((prev) => {
        if (!prev || prev.some((item) => item.channelId === channel.channelId)) return prev;
        return [channel, ...prev];
      });
      select(channel.channelId);
    });
  };

  const markRead = useCallback((channelId: string) => {
    setChannels((prev) => prev?.map((c) => (c.channelId === channelId ? { ...c, unreadCount: 0 } : c)) ?? prev);
  }, []);

  if (!user) return null;
  const active = channels?.find((c) => c.channelId === validActive);

  return (
    <div className="flex h-[calc(100dvh-64px)] bg-neutral-0">
      <aside
        className={clsx(
          'w-full shrink-0 border-r border-neutral-100 md:block md:w-80',
          validActive ? 'hidden' : 'block'
        )}
      >
        <ChannelList
          channels={channels}
          activeId={validActive}
          onSelect={select}
          canSearchStudents={user.role === ROLE.TEACHER}
          studentOptions={studentOptions}
          onSearchStudents={(value) => {
            setStudentOptions([]);
            setStudentSearch(value);
          }}
          onOpenStudent={openStudent}
        />
      </aside>
      <section className={clsx('min-w-0 flex-1', validActive ? 'block' : 'hidden md:block')}>
        {validActive ? (
          <ChatPanel
            key={validActive}
            channelId={validActive}
            title={active?.title ?? null}
            myId={user.id}
            myName={user.fullName}
            isTeacher={user.role === ROLE.TEACHER}
            onRead={markRead}
            onBack={() => router.replace(pathname, { scroll: false })}
          />
        ) : (
          <div className="grid h-full place-items-center p-6 text-center text-neutral-500">
            <div>
              <MessageSquare size={40} className="mx-auto mb-3 text-neutral-300" />
              <p>Chọn một cuộc trò chuyện ở bên trái.</p>
              <p className="mt-1 text-small">
                Kênh lớp có sẵn cho mọi thành viên; kênh theo bài nộp được mở từ màn chấm bài.
              </p>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
