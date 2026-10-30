'use client';

import { useEffect, useState } from 'react';

import { Button, Empty, Skeleton, Tabs, Timeline } from 'antd';
import { RefreshCw } from 'lucide-react';

import { AppDrawer } from '@/components/AppDrawer';
import { ChatPanel } from '@/components/ChatPanel';
import { PresenceDot } from '@/components/PresenceDot';
import { StatusTag } from '@/components/StatusTag';
import { examApi } from '@/lib/api/exam';
import { subscribeRealtime } from '@/lib/realtime';
import { EXAM_EVENT_META, EXAM_STATUS_META } from '@/lib/theme/status';
import type { ExamTimeline, LiveEvent, LiveSnapshot } from '@/lib/types/exam';
import { parseServerDate } from '@/lib/utils/date';
import { useAuthStore } from '@/stores/useAuthStore';

// ─── Xem trực tiếp một sinh viên (BR-26) + dòng thời gian phiên thi (UC-21) ───
// Bài đang viết: REST lấy bản mới nhất rồi nghe `/topic/live/parts/{partId}/students/{studentId}`.
// Góp ý trực tiếp = chat kênh `submission-{id}` (sinh viên thấy ngay dưới ô soạn).

export interface LiveTarget {
  partId: number;
  partTitle: string;
  studentId: number;
  studentName: string;
  /** Phiên thi (phần ở chế độ thi, đã bắt đầu) → có thêm tab dòng thời gian. */
  sessionId: number | null;
}

const time = (iso: string) => parseServerDate(iso).format('HH:mm:ss');

function LiveEssay({ target }: { target: LiveTarget }) {
  const [snap, setSnap] = useState<LiveSnapshot | null>(null);

  useEffect(() => {
    let cancelled = false;
    examApi
      .live(target.partId, target.studentId)
      .then((s) => {
        if (!cancelled) setSnap(s);
      })
      .catch(() => {
        // đã toast
      });
    const off = subscribeRealtime(`/topic/live/parts/${target.partId}/students/${target.studentId}`, (payload) => {
      const ev = payload as LiveEvent;
      setSnap((prev) =>
        prev
          ? ev.type === 'SUBMITTED'
            ? { ...prev, source: 'NOT_WORKING', status: 'SUBMITTED', content: '', wordCount: 0 }
            : { ...prev, source: 'LIVE', content: ev.content, wordCount: ev.wordCount, updatedAt: ev.at }
          : prev
      );
    });
    return () => {
      cancelled = true;
      off();
    };
  }, [target.partId, target.studentId]);

  if (!snap) return <Skeleton active paragraph={{ rows: 6 }} />;
  if (snap.source === 'NOT_WORKING') {
    return <Empty description="Sinh viên đã nộp phần này — mở bài nộp ở màn chấm để xem." />;
  }
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-3 text-small text-neutral-500">
        <PresenceDot online={snap.online}>
          <span className="pl-1">{snap.online ? 'Đang trực tuyến' : 'Ngoại tuyến'}</span>
        </PresenceDot>
        <span className="tabular-nums">{snap.wordCount} từ</span>
        {snap.updatedAt && (
          <span>
            {snap.source === 'LIVE' ? 'Cập nhật lúc' : 'Bản nháp lưu lúc'} {time(snap.updatedAt)}
          </span>
        )}
      </div>
      <div className="max-h-[45vh] min-h-40 overflow-y-auto rounded-md border border-neutral-100 bg-neutral-0 p-4 font-serif text-[16px]/[28px] whitespace-pre-wrap text-neutral-900">
        {snap.content || <span className="font-sans text-neutral-400">Sinh viên chưa viết gì.</span>}
      </div>
      <div className="h-80 overflow-hidden rounded-md border border-neutral-100">
        {snap.submissionId === null ? (
          <p className="p-4 text-small text-neutral-500">
            Góp ý trực tiếp mở được sau khi bài của sinh viên được tự lưu lần đầu.
          </p>
        ) : (
          <TeacherChat submissionId={snap.submissionId} />
        )}
      </div>
    </div>
  );
}

function TeacherChat({ submissionId }: { submissionId: number }) {
  const user = useAuthStore((s) => s.user);
  if (!user) return null;
  return (
    <ChatPanel
      key={submissionId}
      channelId={`submission-${submissionId}`}
      title="Góp ý trực tiếp"
      subtitle="Sinh viên thấy ngay dưới ô soạn bài"
      myId={user.id}
      myName={user.fullName}
      isTeacher
    />
  );
}

function SessionTimeline({ sessionId }: { sessionId: number }) {
  const [data, setData] = useState<ExamTimeline | null>(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let cancelled = false;
    examApi
      .timeline(sessionId)
      .then((t) => {
        if (!cancelled) setData(t);
      })
      .catch(() => {
        // đã toast
      });
    return () => {
      cancelled = true;
    };
  }, [sessionId, version]);

  if (!data) return <Skeleton active paragraph={{ rows: 6 }} />;
  const s = data.session;
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="flex flex-wrap items-center gap-2 text-small text-neutral-500">
          <StatusTag meta={EXAM_STATUS_META[s.status]} size="sm" />
          Bắt đầu {time(s.startedAt)} · hết giờ {time(s.endsAt)}
          {s.finishedAt && ` · nộp ${time(s.finishedAt)}`}
        </span>
        <Button size="small" icon={<RefreshCw size={14} />} onClick={() => setVersion((v) => v + 1)}>
          Tải lại
        </Button>
      </div>
      <Timeline
        items={data.events.map((e) => {
          const meta = EXAM_EVENT_META[e.type];
          return {
            key: e.id,
            color: meta.alert ? 'orange' : 'gray',
            children: (
              <span className="text-small">
                <span className="mr-2 text-neutral-500 tabular-nums">{time(e.occurredAt)}</span>
                <span className={meta.alert ? 'font-medium text-warning' : 'text-neutral-800'}>{meta.label}</span>
                {e.type === 'PASTE' && e.pasteChars !== null && (
                  <span className="text-neutral-500"> · {e.pasteChars} ký tự</span>
                )}
                {e.reportedLate && <span className="text-neutral-500"> · lúc mất mạng, gửi bù khi có mạng</span>}
              </span>
            )
          };
        })}
      />
    </div>
  );
}

export function LiveDrawer({ target, onClose }: { target: LiveTarget | null; onClose: () => void }) {
  return (
    <AppDrawer
      open={target !== null}
      onClose={onClose}
      size={720}
      title={target ? `${target.studentName} — ${target.partTitle}` : ''}
      subtitle="Xem trực tiếp bài đang viết và góp ý ngay"
      destroyOnHidden
    >
      {target &&
        (target.sessionId === null ? (
          <LiveEssay target={target} />
        ) : (
          <Tabs
            items={[
              { key: 'live', label: 'Bài đang viết', children: <LiveEssay target={target} /> },
              {
                key: 'timeline',
                label: 'Dòng thời gian thi',
                children: <SessionTimeline sessionId={target.sessionId} />
              }
            ]}
          />
        ))}
    </AppDrawer>
  );
}
