'use client';

import { useEffect, useMemo, useState } from 'react';

import { Button, Card, Empty, Segmented, Select, Skeleton, Table } from 'antd';
import { CheckCircle2, ClipboardCheck, Download, Users } from 'lucide-react';
import { useParams, useSearchParams } from 'next/navigation';

import { BandBadge } from '@/components/BandBadge';
import { BandDistributionChart } from '@/components/BandDistributionChart';
import { CriteriaBars } from '@/components/CriteriaBars';
import { ErrorStatsTable } from '@/components/ErrorStatsTable';
import { StatCard } from '@/components/StatCard';
import { statsApi } from '@/lib/api/stats';
import { formatBand, getBandLevel } from '@/lib/theme/status';
import type { AssignmentStats, PartStats } from '@/lib/types/stats';
import { overallOnTimeRate } from '@/lib/utils/bandDistribution';

import { AssignmentHeader, AssignmentHeaderFallback, useAssignmentHeader } from '../AssignmentSubNav';

// ─── Giáo viên: thống kê một bài (UC-19, FR-63, Prompt 5) ─────────────────────
// Chỉ tính kết quả đã trả (BR-13). Trung bình giữ 2 chữ số như server trả, không làm tròn kiểu IELTS.

/** Giá trị "cả bài" / "mọi người nhận" của ô chọn (antd không hiện nhãn cho `undefined`). */
const ALL = 0;

const percent = (rate: number | null) => (rate === null ? '—' : `${Math.round(rate * 100)}%`);

export default function AssignmentStatsPage() {
  const id = Number(useParams<{ id: string }>().id);
  const { assignment, targets, notFound } = useAssignmentHeader(id);
  // Mở từ thống kê lớp (?lop=) thì lọc sẵn theo lớp đó.
  const lopParam = Number(useSearchParams().get('lop')) || undefined;

  const [classId, setClassId] = useState<number | undefined>(lopParam);
  const [view, setView] = useState<number>(ALL);
  const [exporting, setExporting] = useState(false);
  const [result, setResult] = useState<{ key: number | undefined; stats: AssignmentStats } | null>(null);

  useEffect(() => {
    let cancelled = false;
    statsApi
      .assignmentStats(id, classId)
      .then((stats) => {
        if (!cancelled) setResult({ key: classId, stats });
      })
      .catch(() => {
        // đã toast
      });
    return () => {
      cancelled = true;
    };
  }, [id, classId]);

  const stats = result?.stats ?? null;
  const loading = !result || result.key !== classId;
  const part: PartStats | undefined = stats?.perPart.find((p) => p.partId === view);
  const onTime = useMemo(() => (stats ? overallOnTimeRate(stats.perPart) : null), [stats]);

  if (!assignment || !targets) return <AssignmentHeaderFallback id={id} view="thong-ke" notFound={notFound} />;

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

  const overallBand = stats?.overall.averageBand ?? null;
  // Bài PAPER: điểm thang 10 (BR-31) thay cho band IELTS.
  const paper = assignment.kind === 'PAPER';
  const scoreWord = paper ? 'Điểm' : 'Band';

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <AssignmentHeader
        id={id}
        view="thong-ke"
        paper={assignment.kind === 'PAPER'}
        title={assignment.title}
        description={
          paper
            ? 'Chỉ tính các phần đã trả bài. Điểm thang 10 của từng thành viên (đã gồm cộng / trừ riêng).'
            : 'Chỉ tính các phần đã trả bài. Trung bình giữ 2 chữ số, chưa làm tròn kiểu IELTS.'
        }
        extra={
          <>
            <Select<number>
              className="w-56"
              value={classId ?? ALL}
              onChange={(v) => setClassId(v === ALL ? undefined : v)}
              options={[
                { value: ALL, label: 'Mọi người nhận bài' },
                ...targets.classes.map((c) => ({ value: c.id, label: `${c.code} – ${c.name}` }))
              ]}
            />
            <Button icon={<Download size={16} />} loading={exporting} onClick={exportExcel}>
              Xuất Excel
            </Button>
          </>
        }
      />

      {!stats ? (
        <Skeleton active paragraph={{ rows: 10 }} />
      ) : (
        <div className={`flex flex-col gap-4 ${loading ? 'opacity-60' : ''}`}>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Card styles={{ body: { padding: 20 } }}>
              <div className="flex items-center gap-4">
                {!paper && <BandBadge band={overallBand} size="lg" />}
                <div className="min-w-0">
                  <p className="text-caption font-medium text-neutral-500">{scoreWord} cả bài trung bình</p>
                  <p className="mt-0.5 text-h3 font-semibold text-neutral-900">
                    {overallBand === null
                      ? 'Chưa có'
                      : paper
                        ? `${overallBand.toFixed(2)} / 10`
                        : `${overallBand.toFixed(2)} · ${getBandLevel(overallBand).label}`}
                  </p>
                  <p className="mt-0.5 text-small text-neutral-500">
                    {stats.overall.count} sinh viên đã được trả đủ mọi phần
                  </p>
                </div>
              </div>
            </Card>
            <StatCard label="Sinh viên nhận bài" value={stats.students} icon={<Users size={20} />} />
            <StatCard
              label="Nộp đúng hạn"
              value={percent(onTime)}
              hint="Tính trên lần nộp đầu của mọi phần"
              icon={<CheckCircle2 size={20} />}
            />
            <StatCard
              label="Phần đã trả bài"
              value={stats.perPart.reduce((sum, p) => sum + p.returnedCount, 0)}
              hint={`trên ${stats.perPart.reduce((sum, p) => sum + p.submittedCount, 0)} phần đã nộp`}
              icon={<ClipboardCheck size={20} />}
            />
          </div>

          <Card title="Từng phần" styles={{ body: { padding: 0 } }}>
            <Table<PartStats>
              rowKey="partId"
              pagination={false}
              dataSource={stats.perPart}
              scroll={{ x: 'max-content' }}
              columns={[
                { title: 'Phần', dataIndex: 'title', render: (t: string) => <span className="font-medium">{t}</span> },
                {
                  title: 'Đã nộp',
                  width: 110,
                  render: (_, p) => (
                    <span className="tabular-nums">
                      {p.submittedCount} / {stats.students}
                    </span>
                  )
                },
                { title: 'Nộp muộn', dataIndex: 'lateCount', width: 100 },
                { title: 'Đúng hạn', width: 100, render: (_, p) => percent(p.onTimeRate) },
                { title: 'Đã trả', dataIndex: 'returnedCount', width: 90 },
                {
                  title: `${scoreWord} trung bình`,
                  width: 140,
                  render: (_, p) => (
                    <span className="font-semibold tabular-nums">
                      {p.averageBand === null ? '—' : p.averageBand.toFixed(2)}
                    </span>
                  )
                }
              ]}
            />
          </Card>

          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-h3 font-semibold text-neutral-900">Phân tích theo phần</h2>
            <Segmented<number>
              value={view}
              onChange={setView}
              options={[
                { value: ALL, label: 'Cả bài' },
                ...stats.perPart.map((p) => ({ value: p.partId, label: p.title }))
              ]}
            />
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <Card
              title={paper ? 'Phân bố điểm' : 'Phân bố band'}
              extra={
                <span className="text-small text-neutral-500">
                  {part
                    ? `${part.returnedCount} ${paper ? 'sinh viên' : 'bài'} · trung bình ${paper ? (part.averageBand?.toFixed(2) ?? '—') : formatBand(part.averageBand)}`
                    : `${stats.overall.count} sinh viên đủ phần`}
                </span>
              }
            >
              {(part ? part.bandDistribution : stats.overall.bandDistribution).length > 0 ? (
                <BandDistributionChart
                  distribution={part ? part.bandDistribution : stats.overall.bandDistribution}
                  unit={part ? (paper ? 'sinh viên' : 'bài') : 'sinh viên'}
                  label={scoreWord}
                />
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có bài nào được trả." />
              )}
            </Card>
            <Card title="Điểm trung bình theo tiêu chí">
              {part ? (
                part.criteria.length > 0 ? (
                  <CriteriaBars
                    items={part.criteria.map((c) => ({
                      key: String(c.criterionId),
                      label: c.name,
                      hint: c.code ?? undefined,
                      average: c.average,
                      count: c.count,
                      max: paper ? c.maxScore : undefined
                    }))}
                  />
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Phần này chưa có tiêu chí." />
                )
              ) : (
                <div className="flex flex-col gap-5">
                  {stats.perPart.map((p) => (
                    <div key={p.partId}>
                      <p className="mb-2 text-small font-semibold text-neutral-900">{p.title}</p>
                      <CriteriaBars
                        items={p.criteria.map((c) => ({
                          key: String(c.criterionId),
                          label: c.name,
                          hint: c.code ?? undefined,
                          average: c.average,
                          count: c.count,
                          max: paper ? c.maxScore : undefined
                        }))}
                      />
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>

          <Card
            title="Lỗi hay gặp"
            extra={<span className="text-small text-neutral-500">Mã lỗi đánh dấu nhiều nhất trong bài này</span>}
          >
            {stats.topErrors.length > 0 ? (
              <ErrorStatsTable errors={stats.topErrors} showStudentCount />
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có lỗi nào được đánh dấu." />
            )}
          </Card>
        </div>
      )}
    </div>
  );
}
