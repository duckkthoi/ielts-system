'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { Avatar, Button, Card, Input, Select, Table, Tooltip } from 'antd';
import type { TableColumnsType } from 'antd';
import { AlertCircle, Bell, CheckCircle2, Clock, Download, Search, Wifi } from 'lucide-react';
import { useParams, useRouter } from 'next/navigation';

import { PresenceDot } from '@/components/PresenceDot';
import { StatCard } from '@/components/StatCard';
import { StatusTag } from '@/components/StatusTag';
import { statsApi } from '@/lib/api/stats';
import { gradingPath } from '@/lib/constants';
import { subscribeRealtime } from '@/lib/realtime';
import { SUBMISSION_STATUS_META } from '@/lib/theme/status';
import type { SubmissionStatus } from '@/lib/theme/status';
import type { ProgressCellDto, ProgressMatrix, ProgressRow } from '@/lib/types/stats';
import { applyPresenceEvent, applyProgressEvent, cellKey, submittedPerPart } from '@/lib/utils/progress';
import type { PresenceEvent, ProgressEvent } from '@/lib/utils/progress';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

import { AssignmentHeader, AssignmentHeaderFallback, useAssignmentHeader } from '../AssignmentSubNav';
import { ProgressCell } from './ProgressCell';
import { RemindModal } from './RemindModal';

// ─── Giáo viên: tiến độ một bài theo thời gian thực (UC-16, FR-54, FR-61, Prompt 5) ─
// Trạng thái ban đầu lấy từ REST; sau đó nghe 2 kênh `/topic/assignments/{id}/progress|presence`
// để đổi ô / chấm online ngay. Số liệu tóm tắt vẫn do server tính: có sự kiện thì gọi lại REST
// sau một nhịp ngắn (gộp nhiều sự kiện), kết nối lại sau khi mất mạng cũng gọi lại.

const REFRESH_DELAY_MS = 1_500;
/** Giá trị "tất cả" của ô chọn lớp (antd không hiện nhãn cho `undefined`). */
const ALL = 0;

const STATUS_OPTIONS = (
  ['NOT_STARTED', 'DRAFT', 'SUBMITTED', 'GRADED', 'RETURNED', 'REVISION_REQUESTED'] as SubmissionStatus[]
).map((s) => ({ value: s, label: SUBMISSION_STATUS_META[s].label }));

const LEGEND: (SubmissionStatus | 'LATE')[] = [
  'NOT_STARTED',
  'DRAFT',
  'SUBMITTED',
  'LATE',
  'GRADED',
  'RETURNED',
  'REVISION_REQUESTED'
];

export default function ProgressPage() {
  const router = useRouter();
  const id = Number(useParams<{ id: string }>().id);
  const { assignment, targets, notFound } = useAssignmentHeader(id);

  const [classId, setClassId] = useState<number | undefined>(undefined);
  const [status, setStatus] = useState<SubmissionStatus | undefined>(undefined);
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState<number[]>([]);
  const [remindOpen, setRemindOpen] = useState(false);
  const [exporting, setExporting] = useState(false);
  // Ô vừa đổi → số lần đổi (để chạy lại hiệu ứng pulse).
  const [pulses, setPulses] = useState<Record<string, number>>({});

  // ─── Tải ma trận ───
  const filterKey = `${classId ?? ''}|${status ?? ''}`;
  const [refreshVersion, setRefreshVersion] = useState(0);
  const [result, setResult] = useState<{ key: string; matrix: ProgressMatrix } | null>(null);

  useEffect(() => {
    let cancelled = false;
    statsApi
      .progress(id, { classId, status })
      .then((matrix) => {
        if (!cancelled) setResult({ key: filterKey, matrix });
      })
      .catch(() => {
        // đã toast; giữ dữ liệu cũ
      });
    return () => {
      cancelled = true;
    };
  }, [id, classId, status, filterKey, refreshVersion]);

  const refreshTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const scheduleRefresh = useCallback(() => {
    if (refreshTimer.current) clearTimeout(refreshTimer.current);
    refreshTimer.current = setTimeout(() => setRefreshVersion((v) => v + 1), REFRESH_DELAY_MS);
  }, []);
  useEffect(() => () => void (refreshTimer.current && clearTimeout(refreshTimer.current)), []);

  // ─── Realtime ───
  useEffect(() => {
    const offProgress = subscribeRealtime(`/topic/assignments/${id}/progress`, (payload) => {
      const ev = payload as ProgressEvent;
      setResult((prev) => (prev ? { ...prev, matrix: applyProgressEvent(prev.matrix, ev) } : prev));
      setPulses((prev) => {
        const key = cellKey(ev.studentId, ev.partId);
        return { ...prev, [key]: (prev[key] ?? 0) + 1 };
      });
      scheduleRefresh();
    });
    const offPresence = subscribeRealtime(`/topic/assignments/${id}/presence`, (payload) => {
      const ev = payload as PresenceEvent;
      setResult((prev) => (prev ? { ...prev, matrix: applyPresenceEvent(prev.matrix, ev) } : prev));
    });
    return () => {
      offProgress();
      offPresence();
    };
  }, [id, scheduleRefresh]);

  // Kết nối lại sau khi mất → có thể đã lỡ sự kiện, tải lại cả ma trận.
  const realtimeStatus = useRealtimeStore((s) => s.status);
  const lastStatus = useRef(realtimeStatus);
  useEffect(() => {
    if (realtimeStatus === 'CONNECTED' && lastStatus.current !== 'CONNECTED' && lastStatus.current !== 'CONNECTING') {
      scheduleRefresh();
    }
    lastStatus.current = realtimeStatus;
  }, [realtimeStatus, scheduleRefresh]);

  const matrix = result?.matrix ?? null;
  const loading = result?.key !== filterKey;

  const rows = useMemo(() => {
    if (!matrix) return [];
    const q = keyword.trim().toLowerCase();
    if (!q) return matrix.rows;
    return matrix.rows.filter(
      (r) => r.student.studentCode.toLowerCase().includes(q) || r.student.fullName.toLowerCase().includes(q)
    );
  }, [matrix, keyword]);

  const perPart = useMemo(() => (matrix ? submittedPerPart(matrix) : {}), [matrix]);
  const onlineCount = matrix?.rows.filter((r) => r.online).length ?? 0;
  const viewingCount = matrix?.rows.filter((r) => r.viewingPartId !== null).length ?? 0;

  // Mở màn chấm với bộ lọc hàng đợi theo bài + trạng thái của ô, để nút Trước / Sau đi đúng danh sách.
  const openSubmission = useCallback(
    (cell: ProgressCellDto) => {
      if (cell.submissionId === null) return;
      const queueStatus = cell.status === 'SUBMITTED' ? undefined : cell.status;
      router.push(gradingPath(cell.submissionId, { assignmentId: id, status: queueStatus }));
    },
    [router, id]
  );

  const exportExcel = async () => {
    setExporting(true);
    try {
      await statsApi.exportGrades(id, classId);
    } catch {
      // đã toast
    } finally {
      setExporting(false);
    }
  };

  const columns: TableColumnsType<ProgressRow> = useMemo(
    () => [
      {
        title: 'Sinh viên',
        key: 'student',
        fixed: 'left',
        width: 240,
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
            {part.title}{' '}
            <span className="font-normal text-neutral-500 tabular-nums">
              · {perPart[part.id] ?? 0}/{matrix?.rows.length ?? 0}
            </span>
          </span>
        ),
        key: `part-${part.id}`,
        align: 'center' as const,
        render: (_: unknown, row: ProgressRow) => {
          const cell = row.cells.find((c) => c.partId === part.id);
          return cell ? (
            <ProgressCell
              cell={cell}
              viewing={row.viewingPartId === part.id}
              pulse={pulses[cellKey(row.student.id, part.id)] ?? 0}
              onOpen={openSubmission}
            />
          ) : null;
        }
      }))
    ],
    [matrix, perPart, pulses, openSubmission]
  );

  if (!assignment || !targets) return <AssignmentHeaderFallback id={id} view="tien-do" notFound={notFound} />;

  const live = realtimeStatus === 'CONNECTED';
  const closed = assignment.status === 'CLOSED';
  const summary = matrix?.summary;

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <AssignmentHeader
        id={id}
        view="tien-do"
        paper={assignment.kind === 'PAPER'}
        title={assignment.title}
        description="Theo dõi tình trạng làm bài theo thời gian thực. Bấm vào ô đã nộp để mở bài chấm."
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

      <div className="mb-4 grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
        <StatCard
          label="Hoàn thành"
          value={summary ? `${Math.round(summary.completedRate * 100)}%` : '—'}
          hint={summary ? `${summary.completedCount}/${summary.students} sinh viên nộp đủ mọi phần` : null}
          icon={<CheckCircle2 size={20} />}
        />
        <StatCard
          label="Nộp muộn"
          value={summary?.lateCount ?? '—'}
          hint="Sinh viên có phần nộp sau hạn"
          icon={<Clock size={20} />}
        />
        <StatCard
          label="Quá hạn chưa nộp"
          value={summary?.overdueCount ?? '—'}
          hint="Còn phần phải nộp mà đã qua hạn"
          icon={<AlertCircle size={20} />}
        />
        <StatCard
          label="Đang trực tuyến"
          value={onlineCount}
          hint={`${viewingCount} người đang mở bài này`}
          icon={<Wifi size={20} />}
        />
      </div>

      <Card styles={{ body: { padding: 0 } }}>
        <div className="flex flex-wrap items-center justify-between gap-3 p-4">
          <div className="flex flex-wrap items-center gap-2">
            <Select<number>
              className="w-52"
              value={classId ?? ALL}
              onChange={(v) => {
                setClassId(v === ALL ? undefined : v);
                setSelected([]);
              }}
              options={[
                { value: ALL, label: 'Mọi người nhận bài' },
                ...targets.classes.map((c) => ({ value: c.id, label: `${c.code} – ${c.name}` }))
              ]}
            />
            <Select<SubmissionStatus | ''>
              className="w-44"
              value={status ?? ''}
              onChange={(v) => setStatus(v || undefined)}
              options={[{ value: '', label: 'Mọi trạng thái' }, ...STATUS_OPTIONS]}
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
          <div className="flex flex-wrap items-center gap-2">
            <Tooltip title={closed ? 'Bài đã đóng, không gửi nhắc nhở được' : undefined}>
              <Button icon={<Bell size={16} />} disabled={closed || !matrix} onClick={() => setRemindOpen(true)}>
                {selected.length > 0 ? `Nhắc ${selected.length} người đã chọn` : 'Nhắc nhở'}
              </Button>
            </Tooltip>
            <Button icon={<Download size={16} />} loading={exporting} onClick={exportExcel}>
              Xuất Excel
            </Button>
          </div>
        </div>
        <Table<ProgressRow>
          size="middle"
          rowKey={(r) => r.student.id}
          loading={loading}
          dataSource={rows}
          columns={columns}
          pagination={rows.length > 50 ? { pageSize: 50, showSizeChanger: false } : false}
          scroll={{ x: 'max-content' }}
          rowSelection={{
            selectedRowKeys: selected,
            onChange: (keys) => setSelected(keys as number[])
          }}
          locale={{
            emptyText: status || keyword ? 'Không có sinh viên khớp bộ lọc.' : 'Bài chưa giao cho sinh viên nào.'
          }}
        />
        <div className="flex flex-wrap items-center gap-2 border-t border-neutral-100 p-4">
          {LEGEND.map((s) => (
            <StatusTag key={s} meta={SUBMISSION_STATUS_META[s]} size="sm" />
          ))}
          <span className="inline-flex items-center gap-1 text-caption text-neutral-500">
            <Clock size={12} /> nộp muộn
          </span>
          <span className="inline-flex items-center gap-1 text-caption text-neutral-500">
            <AlertCircle size={12} className="text-error" /> quá hạn chưa nộp
          </span>
          <span className="inline-flex items-center gap-1 text-caption text-neutral-500">
            <span className="size-2.5 rounded-sm ring-2 ring-presence-online" /> đang mở phần đó
          </span>
        </div>
      </Card>

      {matrix && remindOpen && (
        <RemindModal
          assignmentId={id}
          matrix={matrix}
          selected={selected}
          open={remindOpen}
          onClose={(sent) => {
            setRemindOpen(false);
            if (sent) setSelected([]);
          }}
        />
      )}
    </div>
  );
}
