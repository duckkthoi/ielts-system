'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { Avatar, Card, Input, Segmented, Select, Table } from 'antd';
import type { TableColumnsType } from 'antd';
import { Search } from 'lucide-react';
import { useParams } from 'next/navigation';

import { PresenceDot } from '@/components/PresenceDot';
import { examApi } from '@/lib/api/exam';
import { statsApi } from '@/lib/api/stats';
import { subscribeRealtime } from '@/lib/realtime';
import type { ExamMonitor, ExamPush } from '@/lib/types/exam';
import type { ProgressMatrix, ProgressRow } from '@/lib/types/stats';
import { applyExamPush, examCellKey, isWorking, mergeFeed, monitorSummary, prependFeed } from '@/lib/utils/examMonitor';
import { applyPresenceEvent, applyProgressEvent } from '@/lib/utils/progress';
import type { PresenceEvent, ProgressEvent } from '@/lib/utils/progress';
import { serverNow } from '@/lib/utils/serverClock';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

import { AssignmentHeader, AssignmentHeaderFallback, useAssignmentHeader } from '../AssignmentSubNav';
import { EventFeed } from './EventFeed';
import { LiveDrawer } from './LiveDrawer';
import type { LiveTarget } from './LiveDrawer';
import { MonitorCell } from './MonitorCell';
import { MonitorStats } from './MonitorStats';
import { PaperDocsCard } from './PaperDocsCard';

// ─── Giáo viên: giám sát làm bài theo lớp (UC-21, BR-25, BR-26) ───────────────
// Hàng = người nhận bài (ma trận tiến độ), mỗi ô ghép thêm phiên thi nếu phần ở chế độ thi.
// Ban đầu lấy từ REST, sau đó nghe `/topic/assignments/{id}/progress|presence|exam`; sự kiện của phiên chưa có
// trong bảng (vừa bắt đầu) hoặc kết nối lại → tải lại REST. Bảng tin nạp sẵn 50 sự kiện gần nhất. "Xem" → LiveDrawer;
// bài Paper có thêm khung tài liệu nhóm → xem trực tiếp + nhận xét trên tài liệu (PaperDocsCard, H4).

const REFRESH_DELAY_MS = 1_000;
/** Giá trị "tất cả" của ô chọn lớp (antd không hiện nhãn cho `undefined`). */
const ALL = 0;
type Scope = 'working' | 'all';

export default function MonitorPage() {
  const id = Number(useParams<{ id: string }>().id);
  const { assignment, targets, notFound } = useAssignmentHeader(id);

  const [classId, setClassId] = useState<number | undefined>(undefined);
  const [keyword, setKeyword] = useState('');
  const [scope, setScope] = useState<Scope>('working');
  const [target, setTarget] = useState<LiveTarget | null>(null);
  const [feed, setFeed] = useState<ExamPush[]>([]);
  const [now, setNow] = useState(serverNow);

  // ─── Tải dữ liệu ───
  const [version, setVersion] = useState(0);
  const [data, setData] = useState<{ key: string; matrix: ProgressMatrix; monitor: ExamMonitor } | null>(null);
  const filterKey = String(classId ?? '');

  useEffect(() => {
    let cancelled = false;
    Promise.all([statsApi.progress(id, { classId }), examApi.monitor(id, classId), examApi.recentEvents(id, classId)])
      .then(([matrix, monitor, recent]) => {
        if (cancelled) return;
        setData({ key: filterKey, matrix, monitor });
        setFeed((live) => mergeFeed(recent, live));
      })
      .catch(() => {
        // đã toast; giữ dữ liệu cũ
      });
    return () => {
      cancelled = true;
    };
  }, [id, classId, filterKey, version]);

  const refreshTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const scheduleRefresh = useCallback(() => {
    if (refreshTimer.current) clearTimeout(refreshTimer.current);
    refreshTimer.current = setTimeout(() => setVersion((v) => v + 1), REFRESH_DELAY_MS);
  }, []);
  useEffect(() => () => void (refreshTimer.current && clearTimeout(refreshTimer.current)), []);

  useEffect(() => {
    const timer = setInterval(() => setNow(serverNow()), 1_000);
    return () => clearInterval(timer);
  }, []);

  // ─── Realtime ───
  useEffect(() => {
    const offProgress = subscribeRealtime(`/topic/assignments/${id}/progress`, (payload) => {
      const ev = payload as ProgressEvent;
      setData((prev) => (prev ? { ...prev, matrix: applyProgressEvent(prev.matrix, ev) } : prev));
    });
    const offPresence = subscribeRealtime(`/topic/assignments/${id}/presence`, (payload) => {
      const ev = payload as PresenceEvent;
      setData((prev) => (prev ? { ...prev, matrix: applyPresenceEvent(prev.matrix, ev) } : prev));
    });
    const offExam = subscribeRealtime(`/topic/assignments/${id}/exam`, (payload) => {
      const push = payload as ExamPush;
      setFeed((prev) => prependFeed(prev, push));
      setData((prev) => {
        if (!prev) return prev;
        const res = applyExamPush(prev.monitor.rows, push);
        if (res.unknown) scheduleRefresh();
        return { ...prev, monitor: { ...prev.monitor, rows: res.rows } };
      });
    });
    return () => {
      offProgress();
      offPresence();
      offExam();
    };
  }, [id, scheduleRefresh]);

  // Kết nối lại sau khi mất → có thể đã lỡ sự kiện, tải lại.
  const realtimeStatus = useRealtimeStore((s) => s.status);
  const lastStatus = useRef(realtimeStatus);
  useEffect(() => {
    if (realtimeStatus === 'CONNECTED' && lastStatus.current !== 'CONNECTED' && lastStatus.current !== 'CONNECTING') {
      scheduleRefresh();
    }
    lastStatus.current = realtimeStatus;
  }, [realtimeStatus, scheduleRefresh]);

  // ─── Dữ liệu hiển thị ───
  const matrix = data?.matrix ?? null;
  const examRows = useMemo(() => data?.monitor.rows ?? [], [data]);
  const exams = useMemo(() => new Map(examRows.map((r) => [examCellKey(r.studentId, r.partId), r])), [examRows]);
  const summary = useMemo(() => monitorSummary(examRows), [examRows]);
  const examParts = useMemo(() => data?.monitor.parts ?? [], [data]);

  const rows = useMemo(() => {
    if (!matrix) return [];
    const q = keyword.trim().toLowerCase();
    return matrix.rows.filter(
      (r) =>
        (scope === 'all' || isWorking(r, exams)) &&
        (!q || r.student.studentCode.toLowerCase().includes(q) || r.student.fullName.toLowerCase().includes(q))
    );
  }, [matrix, keyword, scope, exams]);

  // Topic phát mọi sinh viên của bài; khi lọc lớp chỉ giữ sự kiện của sinh viên trong bảng.
  const visibleFeed = useMemo(() => {
    if (classId === undefined || !matrix) return feed;
    const ids = new Set(matrix.rows.map((r) => r.student.id));
    return feed.filter((e) => ids.has(e.studentId));
  }, [feed, classId, matrix]);

  const open = useCallback(
    (studentId: number, studentName: string, partId: number) => {
      const part = matrix?.parts.find((p) => p.id === partId);
      setTarget({
        partId,
        partTitle: part?.title ?? '',
        studentId,
        studentName,
        sessionId: exams.get(examCellKey(studentId, partId))?.sessionId ?? null
      });
    },
    [matrix, exams]
  );

  const columns: TableColumnsType<ProgressRow> = useMemo(
    () => [
      {
        title: 'Sinh viên',
        key: 'student',
        fixed: 'left',
        width: 220,
        render: (_, row) => {
          const viewing = matrix?.parts.find((p) => p.id === row.viewingPartId);
          return (
            <div className="flex items-center gap-2.5">
              <PresenceDot online={row.online}>
                <Avatar size="small" className="bg-brand-50! text-brand-700!">
                  {row.student.fullName.split(' ').pop()?.charAt(0)}
                </Avatar>
              </PresenceDot>
              <div className="min-w-0">
                <p className="truncate font-medium text-neutral-900">{row.student.fullName}</p>
                <p className="truncate font-mono text-caption text-neutral-500">
                  {row.student.studentCode}
                  {viewing && <span className="ml-1.5 font-sans text-success">· đang mở {viewing.title}</span>}
                </p>
              </div>
            </div>
          );
        }
      },
      ...(matrix?.parts ?? []).map((part) => ({
        title: (
          <span className="whitespace-nowrap">
            {part.title}
            {examParts.some((p) => p.id === part.id) && <span className="ml-1.5 font-normal text-error">· thi</span>}
          </span>
        ),
        key: `part-${part.id}`,
        render: (_: unknown, row: ProgressRow) => (
          <MonitorCell
            cell={row.cells.find((c) => c.partId === part.id)}
            exam={exams.get(examCellKey(row.student.id, part.id))}
            viewing={row.viewingPartId === part.id}
            now={now}
            onOpen={() => open(row.student.id, row.student.fullName, part.id)}
          />
        )
      }))
    ],
    [matrix, examParts, exams, now, open]
  );

  if (!assignment || !targets) return <AssignmentHeaderFallback id={id} view="giam-sat" notFound={notFound} />;

  const live = realtimeStatus === 'CONNECTED';
  const onlineCount = matrix?.rows.filter((r) => r.online).length ?? 0;

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <AssignmentHeader
        id={id}
        view="giam-sat"
        paper={assignment.kind === 'PAPER'}
        title={assignment.title}
        description="Theo dõi sinh viên đang làm bài: xem trực tiếp bài đang viết, góp ý ngay, cảnh báo rời trang / dán văn bản khi thi."
        extra={
          <span
            className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-small font-medium ${
              live
                ? 'border-success-border bg-success-bg text-success'
                : 'border-warning-border bg-warning-bg text-warning'
            }`}
            role="status"
          >
            <span className={`size-2 rounded-full ${live ? 'bg-presence-online' : 'bg-presence-away'}`} />
            {live ? 'Cập nhật trực tiếp' : 'Đang kết nối lại…'}
          </span>
        }
      />

      <MonitorStats online={onlineCount} hasExam={examParts.length > 0} summary={summary} />
      {assignment.kind === 'PAPER' && <PaperDocsCard assignmentId={id} parts={assignment.parts} />}

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
        <Card styles={{ body: { padding: 0 } }}>
          <div className="flex flex-wrap items-center gap-2 p-4">
            <Select<number>
              className="w-52"
              value={classId ?? ALL}
              onChange={(v) => setClassId(v === ALL ? undefined : v)}
              options={[
                { value: ALL, label: 'Mọi người nhận bài' },
                ...targets.classes.map((c) => ({ value: c.id, label: `${c.code} – ${c.name}` }))
              ]}
            />
            <Segmented<Scope>
              value={scope}
              onChange={setScope}
              options={[
                { value: 'working', label: 'Đang làm bài' },
                { value: 'all', label: 'Tất cả' }
              ]}
            />
            <Input
              allowClear
              className="w-56"
              prefix={<Search size={14} className="text-neutral-400" />}
              placeholder="Tìm mã SV hoặc họ tên"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
          <Table<ProgressRow>
            size="middle"
            rowKey={(r) => r.student.id}
            loading={data?.key !== filterKey}
            dataSource={rows}
            columns={columns}
            pagination={rows.length > 50 ? { pageSize: 50, showSizeChanger: false } : false}
            scroll={{ x: 'max-content' }}
            locale={{
              emptyText:
                scope === 'working'
                  ? 'Chưa có sinh viên nào đang làm bài. Chọn "Tất cả" để xem mọi người nhận bài.'
                  : 'Không có sinh viên khớp bộ lọc.'
            }}
          />
        </Card>
        <EventFeed feed={visibleFeed} onOpen={(e) => open(e.studentId, e.studentName, e.partId)} />
      </div>

      <LiveDrawer target={target} onClose={() => setTarget(null)} />
    </div>
  );
}
