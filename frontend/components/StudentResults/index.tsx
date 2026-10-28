'use client';

import { useMemo } from 'react';

import Link from 'next/link';

import { Card, Empty, Table } from 'antd';
import { Clock, FileCheck } from 'lucide-react';

import { BandBadge } from '@/components/BandBadge';
import { BandDistributionChart } from '@/components/BandDistributionChart';
import { BandTrendChart } from '@/components/BandTrendChart';
import { CriteriaBars } from '@/components/CriteriaBars';
import { ErrorStatsTable } from '@/components/ErrorStatsTable';
import { StatCard } from '@/components/StatCard';
import { CRITERION_CODE_LABEL, TASK_TYPE_LABEL } from '@/lib/constants';
import { formatBand, getBandLevel } from '@/lib/theme/status';
import type { AssignmentBand, BandCount, PartBand, StudentStats } from '@/lib/types/stats';
import { buildBandTrend } from '@/lib/utils/bandTrend';
import { formatDate } from '@/lib/utils/date';

// ─── Kết quả học tập của một sinh viên (UC-28, Prompt 8) ──────────────────────
// Dùng chung: sinh viên xem mình (/sinh-vien/ket-qua) và giáo viên xem một sinh viên (thống kê lớp).
// Chỉ có kết quả đã trả (BR-13) — server đã lọc. `partHref` bỏ trống thì không có link sang kết quả từng phần.

interface Props {
  stats: StudentStats;
  assignmentHref?: (assignmentId: number) => string;
  partHref?: (part: PartBand) => string;
}

export function StudentResults({ stats, assignmentHref, partHref }: Props) {
  const trend = useMemo(() => buildBandTrend(stats.assignments, stats.parts), [stats]);
  const bandDistribution = useMemo(() => {
    const counts = new Map<number, number>();
    for (const assignment of stats.assignments) {
      if (assignment.complete && assignment.band !== null) {
        counts.set(assignment.band, (counts.get(assignment.band) ?? 0) + 1);
      }
    }
    return [...counts].map(([band, count]): BandCount => ({ band, count })).sort((a, b) => a.band - b.band);
  }, [stats.assignments]);
  const partsByAssignment = useMemo(() => {
    const map = new Map<number, PartBand[]>();
    for (const p of stats.parts) map.set(p.assignmentId, [...(map.get(p.assignmentId) ?? []), p]);
    return map;
  }, [stats]);
  // Bảng: bài trả gần nhất lên đầu (server sắp tăng dần cho biểu đồ).
  const assignmentRows = useMemo(() => [...stats.assignments].reverse(), [stats]);

  const level = stats.averageBand === null ? null : getBandLevel(stats.averageBand);
  const completeCount = stats.assignments.filter((a) => a.complete).length;
  const latePercent = stats.submittedCount > 0 ? Math.round((stats.lateCount / stats.submittedCount) * 100) : 0;

  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-4 sm:grid-cols-3">
        <Card styles={{ body: { padding: 20 } }}>
          <div className="flex items-center gap-4">
            <BandBadge band={stats.averageBand} size="lg" />
            <div className="min-w-0">
              <p className="text-caption font-medium text-neutral-500">Band trung bình</p>
              <p className="mt-0.5 text-h3 font-semibold text-neutral-900">{level ? level.label : 'Chưa có'}</p>
              <p className="mt-0.5 text-small text-neutral-500">
                {stats.averageBand !== null
                  ? `${stats.averageBand.toFixed(2)} trên ${completeCount} bài đã đủ kết quả`
                  : 'Cần một bài được trả đủ mọi phần'}
              </p>
            </div>
          </div>
        </Card>
        <StatCard
          label="Phần đã nộp"
          value={stats.submittedCount}
          hint={`${stats.returnedCount} phần đã có kết quả`}
          icon={<FileCheck size={20} />}
        />
        <StatCard
          label="Nộp muộn"
          value={stats.lateCount}
          hint={stats.submittedCount > 0 ? `${latePercent}% số phần đã nộp` : 'Chưa nộp phần nào'}
          icon={<Clock size={20} />}
        />
      </div>

      {stats.assignments.length === 0 ? (
        <Card>
          <Empty description="Chưa có bài nào được trả kết quả. Khi giáo viên trả bài, band và nhận xét sẽ hiện ở đây." />
        </Card>
      ) : (
        <>
          <div className="grid gap-4 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
            <div className="flex flex-col gap-4">
              <Card title="Band theo thời gian">
                {trend.length > 0 ? (
                  <BandTrendChart series={trend} />
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có band để vẽ biểu đồ." />
                )}
              </Card>
              <Card title="Phân bố band">
                {bandDistribution.length > 0 ? (
                  <BandDistributionChart distribution={bandDistribution} unit="bài" />
                ) : (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description="Cần có bài được chấm đủ mọi phần để xem phân bố band."
                  />
                )}
              </Card>
            </div>
            <Card title="Trung bình theo tiêu chí">
              {stats.criteria.length > 0 ? (
                <CriteriaBars
                  items={stats.criteria.map((c) => ({
                    key: c.code,
                    label: CRITERION_CODE_LABEL[c.code],
                    hint: c.code,
                    average: c.average,
                    count: c.count
                  }))}
                />
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có điểm theo tiêu chí IELTS." />
              )}
            </Card>
          </div>

          <Card
            title="Hồ sơ lỗi"
            extra={<span className="text-small text-neutral-500">Mã lỗi giáo viên đánh dấu nhiều nhất</span>}
          >
            {stats.errorProfile.length > 0 ? (
              <ErrorStatsTable errors={stats.errorProfile} />
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có lỗi nào được đánh dấu." />
            )}
          </Card>

          <Card title="Kết quả từng bài" styles={{ body: { padding: 0 } }}>
            <Table<AssignmentBand>
              rowKey="assignmentId"
              pagination={false}
              dataSource={assignmentRows}
              scroll={{ x: 'max-content' }}
              expandable={{
                expandedRowRender: (row) => (
                  <PartList parts={partsByAssignment.get(row.assignmentId) ?? []} partHref={partHref} />
                )
              }}
              columns={[
                {
                  title: 'Bài tập',
                  dataIndex: 'title',
                  render: (title: string, row) =>
                    assignmentHref ? (
                      <Link href={assignmentHref(row.assignmentId)} className="font-medium">
                        {title}
                      </Link>
                    ) : (
                      <span className="font-medium text-neutral-900">{title}</span>
                    )
                },
                {
                  title: 'Phần đã trả',
                  width: 110,
                  render: (_, row) => (
                    <span className="tabular-nums">
                      {row.returnedParts} / {row.partCount}
                    </span>
                  )
                },
                {
                  title: 'Band cả bài',
                  width: 130,
                  render: (_, row) =>
                    row.complete ? (
                      <BandBadge band={row.band} />
                    ) : (
                      <span className="text-small text-neutral-500">Chưa đủ phần</span>
                    )
                },
                { title: 'Trả gần nhất', dataIndex: 'returnedAt', width: 120, render: (at: string) => formatDate(at) }
              ]}
            />
          </Card>
        </>
      )}
    </div>
  );
}

function PartList({ parts, partHref }: { parts: PartBand[]; partHref?: (part: PartBand) => string }) {
  return (
    <ul className="m-0 flex list-none flex-col gap-2 p-0">
      {parts.map((p) => (
        <li key={p.partId} className="flex flex-wrap items-center gap-x-3 gap-y-1">
          <BandBadge band={p.band} size="sm" />
          <span className="font-medium text-neutral-900">{p.partTitle}</span>
          <span className="text-small text-neutral-500">
            {TASK_TYPE_LABEL[p.taskType]} · band {formatBand(p.band)} · trả {formatDate(p.returnedAt)}
          </span>
          {partHref && (
            <Link href={partHref(p)} className="text-small">
              Xem kết quả
            </Link>
          )}
        </li>
      ))}
    </ul>
  );
}
