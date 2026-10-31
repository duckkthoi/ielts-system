'use client';

import { useMemo, useState } from 'react';

import { AutoComplete, Badge, Empty, Skeleton } from 'antd';
import clsx from 'clsx';
import { FileText, MessageCircle, Search, Users } from 'lucide-react';

import type { ChannelSummary } from '@/lib/types/chat';
import { formatRelative } from '@/lib/utils/date';

import { CHANNEL_GROUPS } from './const';

// ─── Cột trái: một ô lọc cuộc trò chuyện và tìm sinh viên để mở chat riêng ────

export function ChannelList({
  channels,
  activeId,
  onSelect,
  canSearchStudents,
  studentOptions,
  onSearchStudents,
  onOpenStudent
}: {
  channels: ChannelSummary[] | null;
  activeId: string | null;
  onSelect: (channelId: string) => void;
  canSearchStudents: boolean;
  studentOptions: { value: string; studentId: number; label: string }[];
  onSearchStudents: (value: string) => void;
  onOpenStudent: (studentId: number) => void;
}) {
  const [keyword, setKeyword] = useState('');

  const filtered = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    return (channels ?? []).filter((c) => !q || c.title.toLowerCase().includes(q));
  }, [channels, keyword]);

  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-neutral-100 p-4">
        <h1 className="mb-3 text-h2 font-semibold text-neutral-900">Trao đổi</h1>
        <AutoComplete
          className="w-full"
          value={keyword}
          options={canSearchStudents ? studentOptions : []}
          filterOption={false}
          onChange={(value) => {
            setKeyword(value);
            if (canSearchStudents) onSearchStudents(value);
          }}
          onSelect={(_, option) => {
            setKeyword('');
            onSearchStudents('');
            onOpenStudent(option.studentId);
          }}
          placeholder={canSearchStudents ? 'Tìm cuộc trò chuyện hoặc sinh viên' : 'Tìm cuộc trò chuyện'}
          notFoundContent={null}
          allowClear
          prefix={<Search size={14} className="text-neutral-400" />}
        />
      </div>
      <div className="min-h-0 flex-1 overflow-y-auto p-2">
        {!channels ? (
          <div className="p-2">
            <Skeleton active avatar paragraph={{ rows: 1 }} />
            <Skeleton active avatar paragraph={{ rows: 1 }} />
          </div>
        ) : filtered.length === 0 ? (
          <Empty
            className="mt-8"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={keyword ? 'Không có cuộc trò chuyện khớp.' : 'Chưa có cuộc trò chuyện nào.'}
          />
        ) : (
          CHANNEL_GROUPS.map((group) => {
            const items = filtered.filter((c) => c.type === group.type);
            if (items.length === 0) return null;
            return (
              <section key={group.type} className="mb-2">
                <p className="px-2 pt-2 pb-1 text-caption font-semibold tracking-wide text-neutral-500 uppercase">
                  {group.label}
                </p>
                {items.map((c) => (
                  <ChannelItem key={c.channelId} channel={c} active={c.channelId === activeId} onSelect={onSelect} />
                ))}
              </section>
            );
          })
        )}
      </div>
    </div>
  );
}

function ChannelItem({
  channel,
  active,
  onSelect
}: {
  channel: ChannelSummary;
  active: boolean;
  onSelect: (channelId: string) => void;
}) {
  const unread = channel.unreadCount > 0;
  return (
    <button
      type="button"
      onClick={() => onSelect(channel.channelId)}
      className={clsx(
        'flex w-full cursor-pointer items-start gap-3 rounded-md p-2 text-left',
        active ? 'bg-brand-50' : 'hover:bg-neutral-25'
      )}
      aria-current={active ? 'true' : undefined}
    >
      <span
        className={clsx(
          'grid size-9 shrink-0 place-items-center rounded-full',
          channel.type === 'CLASSROOM'
            ? 'bg-accent-50 text-accent-500'
            : channel.type === 'DIRECT'
              ? 'bg-success-50 text-success-600'
              : 'bg-brand-50 text-brand-600'
        )}
      >
        {channel.type === 'CLASSROOM' ? (
          <Users size={16} />
        ) : channel.type === 'DIRECT' ? (
          <MessageCircle size={16} />
        ) : (
          <FileText size={16} />
        )}
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-baseline justify-between gap-2">
          <span className={clsx('truncate text-neutral-900', unread ? 'font-semibold' : 'font-medium')}>
            {channel.title}
          </span>
          {channel.lastMessage && (
            <span className="shrink-0 text-caption text-neutral-400">
              {formatRelative(channel.lastMessage.createdAt)}
            </span>
          )}
        </span>
        <span className="flex items-center justify-between gap-2">
          <span className={clsx('truncate text-small', unread ? 'text-neutral-900' : 'text-neutral-500')}>
            {channel.lastMessage
              ? `${channel.lastMessage.senderName}: ${channel.lastMessage.preview}`
              : 'Chưa có tin nhắn'}
          </span>
          {unread && <Badge count={channel.unreadCount} size="small" />}
        </span>
      </span>
    </button>
  );
}
