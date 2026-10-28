'use client';

import { useEffect, useState } from 'react';

import { Button, Drawer, Empty, List, Spin, Tag } from 'antd';
import { Activity, Download } from 'lucide-react';

import { activityApi } from '@/lib/api/activity';
import { subscribeRealtime } from '@/lib/realtime';
import type { StudentActivityEvent, StudentActivityType } from '@/lib/types/activity';
import { formatDateTime } from '@/lib/utils/date';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

const EVENT_META: Record<StudentActivityType, { label: string; color: string }> = {
  PAGE_ENTERED: { label: 'Vào màn làm bài', color: 'processing' },
  PAGE_LEFT: { label: 'Rời màn làm bài', color: 'default' },
  TAB_HIDDEN: { label: 'Chuyển khỏi tab IELTS', color: 'warning' },
  TAB_VISIBLE: { label: 'Quay lại tab IELTS', color: 'success' }
};

export function ActivityDrawer({
  assignmentId,
  classId,
  open,
  onClose
}: {
  assignmentId: number;
  classId?: number;
  open: boolean;
  onClose: () => void;
}) {
  const realtimeStatus = useRealtimeStore((s) => s.status);
  const queryKey = `${assignmentId}|${classId ?? ''}|${realtimeStatus}`;
  const [feed, setFeed] = useState<{ key: string; events: StudentActivityEvent[] }>({ key: '', events: [] });
  const events = feed.key === queryKey ? feed.events : [];
  const loading = open && feed.key !== queryKey;

  useEffect(() => {
    if (!open) return;

    let cancelled = false;
    activityApi
      .teacherActivity(assignmentId, { classId, limit: 200 })
      .then((latest) => {
        if (!cancelled) {
          setFeed((current) => ({
            key: queryKey,
            events: mergeEvents(latest, current.key === queryKey ? current.events : [])
          }));
        }
      })
      .catch(() => {
        if (!cancelled) {
          setFeed((current) =>
            current.key === queryKey ? current : { key: queryKey, events: [] }
          );
        }
      });

    const unsubscribe = subscribeRealtime(`/topic/assignments/${assignmentId}/activity`, (payload) => {
      const event = payload as StudentActivityEvent;
      if (classId === undefined) {
        setFeed((current) => ({
          key: queryKey,
          events: mergeEvents([event], current.key === queryKey ? current.events : [])
        }));
        return;
      }
      void activityApi
        .teacherActivity(assignmentId, { classId, limit: 200 })
        .then((latest) => {
          if (!cancelled) setFeed({ key: queryKey, events: latest });
        })
        .catch(() => undefined);
    });

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [assignmentId, classId, open, queryKey]);

  return (
    <Drawer
      title={
        <span className="inline-flex items-center gap-2">
          <Activity size={18} /> Hoạt động khi làm bài
        </span>
      }
      open={open}
      onClose={onClose}
      width={480}
    >
      <p className="mb-4 text-small text-neutral-500">
        Ghi nhận khi học sinh vào/rời màn làm bài hoặc tab IELTS chuyển nền rồi quay lại. Trình duyệt không cung cấp
        địa chỉ trang bên ngoài mà học sinh mở.
      </p>
      <div className="mb-3 flex justify-end">
        <Button
          size="small"
          icon={<Download size={14} />}
          disabled={events.length === 0}
          onClick={() => downloadActivityCsv(assignmentId, events)}
        >
          Tải CSV ({events.length})
        </Button>
      </div>
      {loading && events.length === 0 ? (
        <div className="flex justify-center py-10">
          <Spin />
        </div>
      ) : events.length === 0 ? (
        <Empty description="Chưa có hoạt động được ghi nhận." />
      ) : (
        <List
          dataSource={events}
          rowKey={(event) => event.id}
          renderItem={(event) => {
            const meta = EVENT_META[event.type];
            return (
              <List.Item>
                <div className="flex w-full items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate font-medium text-neutral-900">{event.fullName}</p>
                    <p className="font-mono text-caption text-neutral-500">{event.studentCode}</p>
                    <p className="mt-1 text-caption text-neutral-500">{formatDateTime(event.occurredAt)}</p>
                  </div>
                  <Tag color={meta.color}>{meta.label}</Tag>
                </div>
              </List.Item>
            );
          }}
          pagination={false}
        />
      )}
    </Drawer>
  );
}

function mergeEvents(...groups: StudentActivityEvent[][]): StudentActivityEvent[] {
  const unique = new Map<number, StudentActivityEvent>();
  for (const event of groups.flat()) unique.set(event.id, event);
  return [...unique.values()].sort((a, b) => b.occurredAt.localeCompare(a.occurredAt)).slice(0, 200);
}

function downloadActivityCsv(assignmentId: number, events: StudentActivityEvent[]) {
  const rows = [
    ['Mã sinh viên', 'Họ và tên', 'Hoạt động', 'Thời điểm (UTC)'],
    ...events.map((event) => [event.studentCode, event.fullName, EVENT_META[event.type].label, event.occurredAt])
  ];
  const csv = `\uFEFF${rows.map((row) => row.map(csvCell).join(',')).join('\r\n')}`;
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = `hoat-dong-lam-bai-${assignmentId}-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function csvCell(value: string): string {
  const safe = value.replace(/^[\t\r ]*[=+\-@]/, "'$&");
  return `"${safe.replaceAll('"', '""')}"`;
}
