'use client';

import { useEffect, useState } from 'react';

import { Card, Empty, Progress, Select, Skeleton, Table } from 'antd';
import { BookOpen, CheckCircle2, Users } from 'lucide-react';
import Link from 'next/link';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';

import { BandBadge } from '@/components/BandBadge';
import { PageHeader } from '@/components/PageHeader';
import { StatCard } from '@/components/StatCard';
import { StatusTag } from '@/components/StatusTag';
import { classroomApi } from '@/lib/api/classroom';
import { statsApi } from '@/lib/api/stats';
import { ASSIGNMENT_STATUS_META, getBandLevel } from '@/lib/theme/status';
import { BRAND } from '@/lib/theme/tokens';
import type { ClassroomSummary } from '@/lib/types/classroom';
import type { ClassAssignmentStats, ClassStats, RankingRow, StudentRef } from '@/lib/types/stats';
import { formatDate } from '@/lib/utils/date';

import { StudentStatsDrawer } from './StudentStatsDrawer';

// ─── Giáo viên: thống kê lớp, bảng xếp hạng (FR-65, Prompt 8) ─────────────────
// Lớp đang xem nằm trên URL (?lop=); bỏ trống = lớp đầu tiên. Bấm một hàng xếp hạng → kết quả học tập
// của sinh viên đó (FR-64). Chỉ kết quả đã trả (BR-13); trung bình giữ 2 chữ số.

const avg = (band: number | null) => (band === null ? '—' : band.toFixed(2));

export default function ClassStatsPage() {
  const router = useRouter();
  const pathname = usePathname();
  const lopParam = Number(useSearchParams().get('lop')) || null;

  const [classes, setClasses] = useState<ClassroomSummary[] | null>(null);
  const [result, setResult] = useState<{ id: number; stats: ClassStats } | null>(null);
  const [student, setStudent] = useState<StudentRef | null>(null);

  useEffect(() => {
    let cancelled = false;
    classroomApi
      .listClasses({ size: 200 })
      .then((res) => {
        if (!cancelled) setClasses(res.data);
      })
      .catch(() => {
        if (!cancelled) setClasses([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const classId = lopParam ?? classes?.[0]?.id ?? null;

  useEffect(() => {
    if (classId === null) return;
    let cancelled = false;
    statsApi
      .classStats(classId)
      .then((stats) => {
        if (!cancelled) setResult({ id: classId, stats });
      })
      .catch(() => {
        // đã toast
      });
    return () => {
      cancelled = true;
    };
  }, [classId]);

  const stats = result?.id === classId ? result.stats : null;

  const header = (
    <PageHeader
      title="Thống kê lớp"
      description="Kết quả các bài đã giao cho lớp và bảng xếp hạng theo band cả bài (bài Paper chấm thang 10, không tính vào xếp hạng)."
      extra={
        classes && classes.length > 0 ? (
          <Select<number>
            className="w-64"
            value={classId ?? undefined}
            onChange={(id) => router.replace(`${pathname}?lop=${id}`, { scroll: false })}
            options={classes.map((c) => ({ value: c.id, label: `${c.code} – ${c.name}` }))}
            showSearch={{ optionFilterProp: 'label' }}
          />
        ) : null
      }
    />
  );

  if (classes && classes.length === 0) {
    return (
      <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
        {header}
        <Card>
          <Empty description="Bạn chưa có lớp nào. Tạo lớp ở mục Lớp học để xem thống kê.">
            <Link href="/giao-vien/lop-hoc">Đến Lớp học</Link>
          </Empty>
        </Card>
      </div>
    );
  }

  const assignments = stats?.assignments ?? [];
  const meanCompleted =
    assignments.length > 0 ? assignments.reduce((sum, a) => sum + a.completedRate, 0) / assignments.length : null;

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      {header}
      {!stats ? (
        <Skeleton active paragraph={{ rows: 10 }} />
      ) : (
        <div className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
            <Card className="col-span-2 xl:col-span-1" styles={{ body: { padding: 20 } }}>
              <div className="flex items-center gap-4">
                <BandBadge band={stats.averageBand} size="lg" />
                <div className="min-w-0">
                  <p className="text-caption font-medium text-neutral-500">Band trung bình của lớp</p>
                  <p className="mt-0.5 text-h3 font-semibold text-neutral-900">
                    {stats.averageBand === null
                      ? 'Chưa có'
                      : `${stats.averageBand.toFixed(2)} · ${getBandLevel(stats.averageBand).label}`}
                  </p>
                  <p className="mt-0.5 text-small text-neutral-500">Trên các bài sinh viên đã có đủ kết quả</p>
                </div>
              </div>
            </Card>
            <StatCard label="Thành viên" value={stats.classroom.memberCount} icon={<Users size={20} />} />
            <StatCard label="Bài đã giao cho lớp" value={stats.assignments.length} icon={<BookOpen size={20} />} />
            <StatCard
              label="Hoàn thành trung bình"
              value={meanCompleted === null ? '—' : `${Math.round(meanCompleted * 100)}%`}
              hint="Tỉ lệ thành viên nộp đủ mọi phần, trung bình các bài"
              icon={<CheckCircle2 size={20} />}
            />
          </div>

          <Card title="Các bài đã giao cho lớp" styles={{ body: { padding: 0 } }}>
            <Table<ClassAssignmentStats>
              rowKey="assignmentId"
              pagination={false}
              dataSource={stats.assignments}
              scroll={{ x: 'max-content' }}
              locale={{ emptyText: 'Chưa có bài nào được giao cho lớp này.' }}
              columns={[
                {
                  title: 'Bài tập',
                  dataIndex: 'title',
                  render: (title: string, a) => (
                    <Link href={`/giao-vien/bai-tap/${a.assignmentId}/thong-ke?lop=${stats.classroom.id}`}>
                      <span className="font-medium">{title}</span>
                    </Link>
                  )
                },
                {
                  title: 'Trạng thái',
                  dataIndex: 'status',
                  width: 120,
                  render: (s: ClassAssignmentStats['status']) => (
                    <StatusTag meta={ASSIGNMENT_STATUS_META[s]} size="sm" />
                  )
                },
                {
                  title: 'Hạn nộp',
                  dataIndex: 'dueAt',
                  width: 120,
                  render: (d: string | null) => (d ? formatDate(d) : '—')
                },
                {
                  title: 'Nộp đủ mọi phần',
                  width: 220,
                  render: (_, a) => (
                    <div className="flex items-center gap-3">
                      <Progress
                        className="m-0! flex-1"
                        percent={Math.round(a.completedRate * 100)}
                        showInfo={false}
                        size="small"
                        strokeColor={BRAND[600]}
                      />
                      <span className="w-14 text-right text-small tabular-nums">
                        {a.completedCount}/{stats.classroom.memberCount}
                      </span>
                    </div>
                  )
                },
                { title: 'Đã có đủ kết quả', dataIndex: 'gradedCount', width: 140 },
                {
                  title: 'Band / điểm trung bình',
                  dataIndex: 'averageBand',
                  width: 170,
                  render: (b: number | null, a) => (
                    <span className="font-semibold tabular-nums">
                      {avg(b)}
                      {a.kind === 'PAPER' && b !== null && (
                        <span className="text-small font-normal text-neutral-500"> / 10</span>
                      )}
                    </span>
                  )
                }
              ]}
            />
          </Card>

          <Card
            title="Bảng xếp hạng"
            extra={<span className="text-small text-neutral-500">Bấm một sinh viên để xem kết quả học tập</span>}
            styles={{ body: { padding: 0 } }}
          >
            <Table<RankingRow>
              rowKey={(r) => r.student.id}
              dataSource={stats.ranking}
              pagination={stats.ranking.length > 50 ? { pageSize: 50, showSizeChanger: false } : false}
              scroll={{ x: 'max-content' }}
              onRow={(r) => ({ onClick: () => setStudent(r.student), className: 'cursor-pointer' })}
              locale={{ emptyText: 'Lớp chưa có thành viên.' }}
              columns={[
                {
                  title: 'Hạng',
                  dataIndex: 'rank',
                  width: 70,
                  render: (rank: number | null) =>
                    rank === null ? (
                      <span className="text-neutral-400">—</span>
                    ) : (
                      <span
                        className={`font-semibold tabular-nums ${rank <= 3 ? 'text-brand-700' : 'text-neutral-700'}`}
                      >
                        {rank}
                      </span>
                    )
                },
                {
                  title: 'Sinh viên',
                  key: 'student',
                  render: (_, r) => (
                    <div>
                      <p className="font-medium text-neutral-900">{r.student.fullName}</p>
                      <p className="font-mono text-caption text-neutral-500">{r.student.studentCode}</p>
                    </div>
                  )
                },
                {
                  title: 'Band trung bình',
                  dataIndex: 'averageBand',
                  width: 150,
                  render: (b: number | null) => (
                    <span className="inline-flex items-center gap-2">
                      <BandBadge band={b} size="sm" />
                      <span className="tabular-nums">{avg(b)}</span>
                    </span>
                  )
                },
                { title: 'Bài đủ kết quả', dataIndex: 'gradedAssignments', width: 130 },
                { title: 'Phần đã nộp', dataIndex: 'submittedParts', width: 120 },
                {
                  title: 'Phần nộp muộn',
                  dataIndex: 'lateParts',
                  width: 130,
                  render: (n: number) => <span className={n > 0 ? 'font-medium text-warning' : ''}>{n}</span>
                }
              ]}
            />
          </Card>
        </div>
      )}

      <StudentStatsDrawer student={student} onClose={() => setStudent(null)} />
    </div>
  );
}
