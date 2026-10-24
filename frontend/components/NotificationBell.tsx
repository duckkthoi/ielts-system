'use client';

import { useCallback, useState } from 'react';

import { Badge, Button, Empty, Popover } from 'antd';
import clsx from 'clsx';
import { Bell, ChevronRight } from 'lucide-react';
import { useRouter } from 'next/navigation';

import { IconButton } from '@/components/IconButton';
import { NotificationIcon } from '@/components/NotificationIcon';
import type { NotificationDto } from '@/lib/types/notification';
import { formatRelative } from '@/lib/utils/date';
import { useNotificationStore } from '@/stores/useNotificationStore';

// ─── Chuông thông báo trên header ─────────────────────────────────────────────
// Số chưa đọc + vài thông báo mới nhất; dữ liệu do RealtimeBridge nạp và cập nhật qua WebSocket.

const PREVIEW_LIMIT = 6;
const NOTIFICATIONS_PATH = '/thong-bao';

export function NotificationItem({ item, onOpen }: { item: NotificationDto; onOpen: (item: NotificationDto) => void }) {
  return (
    <button
      type="button"
      onClick={() => onOpen(item)}
      className={clsx(
        'flex w-full cursor-pointer items-start gap-3 rounded-md px-3 py-2.5 text-left transition-colors hover:bg-neutral-25',
        !item.readAt && 'bg-brand-50/60'
      )}
    >
      <NotificationIcon type={item.type} size={32} />
      <span className="min-w-0 flex-1">
        <span className={clsx('block text-neutral-900', !item.readAt && 'font-semibold')}>{item.title}</span>
        {item.body && <span className="line-clamp-2 block text-small text-neutral-500">{item.body}</span>}
        <span className="mt-0.5 block text-caption text-neutral-400">{formatRelative(item.createdAt)}</span>
      </span>
      {!item.readAt && <span className="mt-2 size-2 shrink-0 rounded-full bg-brand-600" aria-label="Chưa đọc" />}
    </button>
  );
}

export function NotificationBell() {
  const router = useRouter();
  const items = useNotificationStore((s) => s.items);
  const unreadCount = useNotificationStore((s) => s.unreadCount);
  const markRead = useNotificationStore((s) => s.markRead);
  const markAllRead = useNotificationStore((s) => s.markAllRead);
  const [open, setOpen] = useState(false);

  const handleOpenItem = useCallback(
    (item: NotificationDto) => {
      setOpen(false);
      if (!item.readAt) markRead(item.id).catch(() => undefined);
      if (item.link) router.push(item.link);
    },
    [markRead, router]
  );

  const handleMarkAll = useCallback(() => {
    markAllRead().catch(() => undefined);
  }, [markAllRead]);

  const handleViewAll = useCallback(() => {
    setOpen(false);
    router.push(NOTIFICATIONS_PATH);
  }, [router]);

  const content = (
    <div className="w-[min(380px,calc(100vw-32px))]">
      <div className="flex items-center justify-between px-3 pt-1 pb-2">
        <span className="text-h3 font-semibold text-neutral-900">Thông báo</span>
        <Button type="link" size="small" onClick={handleMarkAll} disabled={unreadCount === 0}>
          Đánh dấu đã đọc tất cả
        </Button>
      </div>
      {items.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có thông báo nào" className="py-6" />
      ) : (
        <div className="flex max-h-[420px] flex-col gap-0.5 overflow-y-auto">
          {items.slice(0, PREVIEW_LIMIT).map((item) => (
            <NotificationItem key={item.id} item={item} onOpen={handleOpenItem} />
          ))}
        </div>
      )}
      <Button type="link" block className="mt-1" onClick={handleViewAll}>
        Xem tất cả <ChevronRight size={14} />
      </Button>
    </div>
  );

  return (
    <Popover
      content={content}
      trigger="click"
      placement="bottomRight"
      open={open}
      onOpenChange={setOpen}
      arrow={false}
      styles={{ container: { padding: 8 } }}
    >
      <Badge count={unreadCount} size="small" overflowCount={99}>
        <IconButton title="Thông báo" type="text" icon={<Bell size={18} />} />
      </Badge>
    </Popover>
  );
}
