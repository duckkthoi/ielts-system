'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';

import { Button, Card, Empty, Skeleton, Tabs } from 'antd';
import { useRouter } from 'next/navigation';

import { NotificationItem } from '@/components/NotificationBell';
import { notificationApi } from '@/lib/api/notification';
import type { NotificationDto } from '@/lib/types/notification';
import { groupByDay, mergeNotifications } from '@/lib/utils/notifications';
import { useNotificationStore } from '@/stores/useNotificationStore';

import { PAGE_SIZE, TAB } from './const';
import type { NotificationTab } from './type';

// ─── Trang Thông báo: tất cả / chưa đọc, nhóm theo ngày, tải thêm ─────────────
// Danh sách của trang tự tải theo trang; thông báo mới (store.incoming) được chèn lên đầu.

export default function NotificationsPage() {
  const router = useRouter();
  const unreadCount = useNotificationStore((s) => s.unreadCount);
  const markReadInStore = useNotificationStore((s) => s.markRead);
  const markAllInStore = useNotificationStore((s) => s.markAllRead);

  const [tab, setTab] = useState<NotificationTab>(TAB.all);
  const [items, setItems] = useState<NotificationDto[]>([]);
  /** Server còn trang sau không (theo totalRecord lúc tải trang gần nhất). */
  const [hasMore, setHasMore] = useState(false);
  const [page, setPage] = useState(0);
  /** Tab đã tải xong trang đầu; khác tab đang chọn = đang tải. */
  const [loadedTab, setLoadedTab] = useState<NotificationTab | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);

  // Đổi tab → tải lại trang đầu. setState chỉ trong callback của promise (không gọi đồng bộ trong effect).
  useEffect(() => {
    let cancelled = false;
    notificationApi
      .list({ unreadOnly: tab === TAB.unread, page: 0, size: PAGE_SIZE })
      .then((res) => {
        if (cancelled) return;
        setItems(res.data);
        setHasMore(PAGE_SIZE < (res.totalRecord ?? 0));
        setPage(0);
      })
      .catch(() => undefined)
      .finally(() => {
        if (!cancelled) setLoadedTab(tab);
      });
    return () => {
      cancelled = true;
    };
  }, [tab]);

  // Thông báo mới (realtime hoặc lấy bù sau kết nối lại) → chèn lên đầu; tab "Chưa đọc" cũng nhận vì chúng chưa đọc.
  useEffect(
    () =>
      useNotificationStore.subscribe((state, prev) => {
        if (state.incoming === prev.incoming || state.incoming.length === 0) return;
        setItems((current) => {
          const fresh = state.incoming.filter((n) => !current.some((c) => c.id === n.id));
          return fresh.length === 0 ? current : mergeNotifications(current, fresh);
        });
      }),
    []
  );

  const handleLoadMore = useCallback(async () => {
    setLoadingMore(true);
    try {
      const res = await notificationApi.list({ unreadOnly: tab === TAB.unread, page: page + 1, size: PAGE_SIZE });
      setItems((current) => mergeNotifications(current, res.data));
      setHasMore((page + 2) * PAGE_SIZE < (res.totalRecord ?? 0));
      setPage(page + 1);
    } catch {
      // axiosClient đã hiển thị toast lỗi
    } finally {
      setLoadingMore(false);
    }
  }, [tab, page]);

  const handleOpen = useCallback(
    async (item: NotificationDto) => {
      if (!item.readAt) {
        const readAt = new Date().toISOString();
        setItems((current) => current.map((n) => (n.id === item.id ? { ...n, readAt } : n)));
        markReadInStore(item.id).catch(() => undefined);
      }
      if (item.link) router.push(item.link);
    },
    [markReadInStore, router]
  );

  const handleMarkAll = useCallback(async () => {
    try {
      await markAllInStore();
      const readAt = new Date().toISOString();
      setItems((current) => (tab === TAB.unread ? [] : current.map((n) => (n.readAt ? n : { ...n, readAt }))));
      if (tab === TAB.unread) setHasMore(false);
    } catch {
      // axiosClient đã hiển thị toast lỗi
    }
  }, [markAllInStore, tab]);

  const groups = useMemo(() => groupByDay(items), [items]);
  const loading = loadedTab !== tab;

  const list = loading ? (
    <Card>
      <Skeleton active avatar paragraph={{ rows: 2 }} />
      <Skeleton active avatar paragraph={{ rows: 2 }} />
    </Card>
  ) : items.length === 0 ? (
    <Card>
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description={
          tab === TAB.unread
            ? 'Bạn đã đọc hết thông báo'
            : 'Chưa có thông báo nào. Bài tập mới, kết quả chấm và lời nhắc hạn nộp sẽ hiện ở đây.'
        }
      />
    </Card>
  ) : (
    <div className="flex flex-col gap-5">
      {groups.map((group) => (
        <section key={group.group}>
          <h2 className="mb-2 text-caption font-semibold tracking-wide text-neutral-500 uppercase">{group.group}</h2>
          <Card styles={{ body: { padding: 8 } }}>
            <div className="flex flex-col gap-0.5">
              {group.items.map((item) => (
                <NotificationItem key={item.id} item={item} onOpen={handleOpen} />
              ))}
            </div>
          </Card>
        </section>
      ))}
      {hasMore && (
        <Button className="self-center" onClick={handleLoadMore} loading={loadingMore}>
          Xem thêm
        </Button>
      )}
    </div>
  );

  return (
    <div className="mx-auto w-full max-w-200 px-4 py-6 sm:px-6">
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-h1 font-semibold text-neutral-900">Thông báo</h1>
          <p className="text-neutral-500">Theo dõi cập nhật mới nhất về bài tập và kết quả.</p>
        </div>
        <Button type="link" onClick={handleMarkAll} disabled={unreadCount === 0}>
          Đánh dấu đã đọc tất cả
        </Button>
      </div>
      <Tabs
        activeKey={tab}
        onChange={(key) => setTab(key as NotificationTab)}
        items={[
          { key: TAB.all, label: 'Tất cả' },
          { key: TAB.unread, label: `Chưa đọc (${unreadCount})` }
        ]}
      />
      {list}
    </div>
  );
}
