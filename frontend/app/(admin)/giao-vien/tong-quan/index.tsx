'use client';

import { useEffect, useMemo, useState } from 'react';

import { Button, Card, Empty, Progress, Skeleton } from 'antd';
import clsx from 'clsx';
import { BookOpen, ClipboardCheck, PieChart, Plus, School, Users } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import { CountdownTimer } from '@/components/CountdownTimer';
import { NotificationIcon } from '@/components/NotificationIcon';
import { PageHeader } from '@/components/PageHeader';
import { StatCard } from '@/components/StatCard';
import { assignmentApi } from '@/lib/api/assignment';
import { classroomApi } from '@/lib/api/classroom';
import { gradingApi } from '@/lib/api/grading';
import { BRAND } from '@/lib/theme/tokens';
import type { AssignmentSummary } from '@/lib/types/assignment';
import type { ClassroomSummary } from '@/lib/types/classroom';
import type { NotificationDto } from '@/lib/types/notification';
import { sortByDue, summarizeOpenAssignments } from '@/lib/utils/dashboard';
import { formatRelative } from '@/lib/utils/date';
import { serverNow } from '@/lib/utils/serverClock';
import { useAuthStore } from '@/stores/useAuthStore';
import { useNotificationStore } from '@/stores/useNotificationStore';

// ─── Giáo viên: tổng quan (Prompt 8) ──────────────────────────────────────────
// Không có API riêng: ghép từ danh sách bài đang mở, hàng đợi chấm (chỉ lấy tổng số), danh sách lớp
// và thông báo gần nhất (store của chuông — tự cập nhật qua WebSocket nên "Hoạt động gần đây" là realtime).

const ACTIVITY_LIMIT = 8;
const OPEN_LIMIT = 6;

interface DashboardData {
  open: AssignmentSummary[];
  classes: ClassroomSummary[];
  waiting: number;
  grading: number;
}

export default function TeacherDashboardPage() {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const notifications = useNotificationStore((s) => s.items);
  const markRead = useNotificationStore((s) => s.markRead);
  const [data, setData] = useState<DashboardData | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      assignmentApi.list({ status: 'PUBLISHED', size: 100 }),
      classroomApi.listClasses({ size: 200 }),
      gradingApi.queue({ status: 'SUBMITTED', size: 1 }),
      gradingApi.queue({ status: 'GRADED', size: 1 })
    ])
      .then(([open, classes, waiting, grading]) => {
        if (cancelled) return;
        setData({
          open: open.data,
          classes: classes.data,
          waiting: waiting.totalRecord ?? waiting.data.length,
          grading: grading.totalRecord ?? grading.data.length
        });
      })
      .catch(() => {
        if (!cancelled) setData({ open: [], classes: [], waiting: 0, grading: 0 });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const summary = useMemo(() => (data ? summarizeOpenAssignments(data.open, serverNow()) : null), [data]);
  const students = data?.classes.reduce((sum, c) => sum + c.memberCount, 0) ?? 0;
  const activity = notifications.slice(0, ACTIVITY_LIMIT);

  const openNotification = (n: NotificationDto) => {
    if (!n.readAt) void markRead(n.id);
    if (n.link) router.push(n.link);
  };

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader
        title={user ? `Chào ${user.fullName}` : 'Tổng quan'}
        description="Đây là tình hình các lớp và bài tập của bạn."
        extra={
          <Link href="/giao-vien/bai-tap/tao-moi">
            <Button type="primary" icon={<Plus size={16} />}>
              Tạo bài tập
            </Button>
          </Link>
        }
      />

      {!data || !summary ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }, (_, i) => (
            <Card key={i}>
              <Skeleton active paragraph={{ rows: 1 }} />
            </Card>
          ))}
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Link href="/giao-vien/cham-bai" className="block">
              <StatCard
                label="Bài chờ chấm"
                value={data.waiting}
                hint={data.grading > 0 ? `${data.grading} bài đang chấm dở` : 'Bấm để mở hàng đợi'}
                icon={<ClipboardCheck size={20} />}
              />
            </Link>
            <StatCard
              label="Bài tập đang mở"
              value={summary.open}
              hint={summary.dueSoon > 0 ? `${summary.dueSoon} bài hết hạn trong 48 giờ` : 'Không có bài sắp hết hạn'}
              icon={<BookOpen size={20} />}
            />
            <StatCard
              label="Nộp đủ (bài đang mở)"
              value={summary.recipients > 0 ? `${Math.round((summary.completed / summary.recipients) * 100)}%` : '—'}
              hint={`${summary.completed}/${summary.recipients} lượt sinh viên đã nộp đủ`}
              icon={<PieChart size={20} />}
            />
            <StatCard
              label="Lớp học"
              value={data.classes.length}
              hint={`${students} lượt thành viên`}
              icon={<School size={20} />}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
            <Card
              title="Bài đang mở"
              extra={
                <Link href="/giao-vien/bai-tap" className="text-small">
                  Tất cả bài tập
                </Link>
              }
            >
              {data.open.length === 0 ? (
                <Empty description="Chưa có bài nào đang mở. Tạo và giao bài để sinh viên bắt đầu làm." />
              ) : (
                <ul className="m-0 flex list-none flex-col divide-y divide-neutral-100 p-0">
                  {sortByDue(data.open)
                    .slice(0, OPEN_LIMIT)
                    .map((a) => (
                      <li key={a.id} className="flex flex-wrap items-center gap-x-4 gap-y-2 py-3 first:pt-0 last:pb-0">
                        <div className="min-w-0 flex-1 basis-56">
                          <Link href={`/giao-vien/bai-tap/${a.id}/tien-do`} className="font-medium">
                            {a.title}
                          </Link>
                          <div className="mt-1 flex items-center gap-2">
                            <Progress
                              className="m-0! max-w-48 flex-1"
                              percent={
                                a.recipientCount > 0 ? Math.round((a.completedCount / a.recipientCount) * 100) : 0
                              }
                              showInfo={false}
                              size="small"
                              strokeColor={BRAND[600]}
                            />
                            <span className="text-caption text-neutral-500 tabular-nums">
                              {a.completedCount}/{a.recipientCount} nộp đủ
                            </span>
                          </div>
                        </div>
                        {a.dueAt ? (
                          <CountdownTimer dueAt={a.dueAt} />
                        ) : (
                          <span className="text-small text-neutral-400">Không hạn</span>
                        )}
                        <Link
                          href={`/giao-vien/cham-bai?assignmentId=${a.id}`}
                          className={clsx(
                            'w-24 text-right text-small',
                            a.pendingGradingCount === 0 && 'pointer-events-none text-neutral-400!'
                          )}
                        >
                          {a.pendingGradingCount > 0 ? `${a.pendingGradingCount} chờ chấm` : 'Đã chấm hết'}
                        </Link>
                      </li>
                    ))}
                </ul>
              )}
            </Card>

            <Card
              title="Hoạt động gần đây"
              extra={
                <Link href="/thong-bao" className="text-small">
                  Xem tất cả
                </Link>
              }
            >
              {activity.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có hoạt động nào." />
              ) : (
                <ul className="m-0 flex list-none flex-col gap-1 p-0">
                  {activity.map((n) => (
                    <li key={n.id}>
                      <button
                        type="button"
                        onClick={() => openNotification(n)}
                        className="flex w-full cursor-pointer items-start gap-3 rounded-md p-2 text-left hover:bg-neutral-25"
                      >
                        <NotificationIcon type={n.type} size={28} />
                        <span className="min-w-0 flex-1">
                          <span className={clsx('block truncate text-neutral-900', !n.readAt && 'font-semibold')}>
                            {n.title}
                          </span>
                          {n.body && <span className="block truncate text-small text-neutral-500">{n.body}</span>}
                          <span className="block text-caption text-neutral-400">{formatRelative(n.createdAt)}</span>
                        </span>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>

          <Card
            title="Lớp học"
            extra={
              <Link href="/giao-vien/lop-hoc" className="text-small">
                Quản lý lớp
              </Link>
            }
          >
            {data.classes.length === 0 ? (
              <Empty description="Chưa có lớp nào. Tạo lớp và import sinh viên để bắt đầu.">
                <Link href="/giao-vien/lop-hoc">Đến Lớp học</Link>
              </Empty>
            ) : (
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                {data.classes.map((c) => (
                  <div key={c.id} className="flex items-center gap-3 rounded-lg border border-neutral-100 p-3">
                    <span className="grid size-10 shrink-0 place-items-center rounded-md bg-brand-50 text-brand-600">
                      <Users size={18} />
                    </span>
                    <div className="min-w-0 flex-1">
                      <Link href={`/giao-vien/lop-hoc/${c.id}`} className="block truncate font-medium">
                        {c.code} – {c.name}
                      </Link>
                      <p className="text-small text-neutral-500">
                        {c.memberCount} sinh viên · {c.openAssignmentCount} bài đang mở
                      </p>
                    </div>
                    <Link href={`/giao-vien/thong-ke?lop=${c.id}`} className="shrink-0 text-small">
                      Thống kê
                    </Link>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      )}
    </div>
  );
}
