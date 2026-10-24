'use client';

import { useEffect, useState } from 'react';

import { Card, Empty, Skeleton } from 'antd';
import { BookOpen, FileCheck, School, Users } from 'lucide-react';

import { PageHeader } from '@/components/PageHeader';
import { StatCard } from '@/components/StatCard';
import { adminApi } from '@/lib/api/admin';
import { ROLE } from '@/lib/constants';
import { ASSIGNMENT_STATUS_META } from '@/lib/theme/status';
import type { AssignmentStatus } from '@/lib/theme/status';
import type { SystemStats } from '@/lib/types/admin';

import { SubmissionsChart } from './SubmissionsChart';

// ─── Quản trị: tổng quan hệ thống (UC-09) ─────────────────────────────────────

const sum = (values: Record<string, number | undefined>) =>
  Object.values(values).reduce<number>((total, v) => total + (v ?? 0), 0);

export default function AdminOverviewPage() {
  const [stats, setStats] = useState<SystemStats | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    adminApi
      .getStats()
      .then((res) => {
        if (!cancelled) setStats(res);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const header = <PageHeader title="Tổng quan hệ thống" description="Người dùng, lớp học, bài tập và bài nộp." />;

  if (failed) {
    return (
      <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
        {header}
        <Card>
          <Empty description="Không tải được số liệu. Hãy tải lại trang." />
        </Card>
      </div>
    );
  }

  const assignmentHint = stats
    ? (Object.keys(ASSIGNMENT_STATUS_META) as AssignmentStatus[])
        .map((s) => `${stats.assignmentsByStatus[s] ?? 0} ${ASSIGNMENT_STATUS_META[s].label.toLowerCase()}`)
        .join(' · ')
    : null;

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      {header}
      {!stats ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }, (_, i) => (
            <Card key={i}>
              <Skeleton active paragraph={{ rows: 1 }} />
            </Card>
          ))}
        </div>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              label="Người dùng"
              value={sum(stats.usersByRole)}
              hint={`${stats.usersByRole[ROLE.TEACHER] ?? 0} giáo viên · ${stats.usersByRole[ROLE.STUDENT] ?? 0} sinh viên · ${stats.lockedUsers} bị khoá`}
              icon={<Users size={20} />}
            />
            <StatCard label="Lớp học" value={stats.classrooms} icon={<School size={20} />} />
            <StatCard
              label="Bài tập"
              value={sum(stats.assignmentsByStatus)}
              hint={assignmentHint}
              icon={<BookOpen size={20} />}
            />
            <StatCard
              label="Bài nộp hôm nay"
              value={stats.submissionsToday}
              hint={`${stats.activeSessions} phiên đăng nhập đang hoạt động`}
              icon={<FileCheck size={20} />}
            />
          </div>
          <Card
            className="mt-4"
            title="Bài nộp theo ngày"
            extra={<span className="text-small text-neutral-500">14 ngày gần nhất</span>}
          >
            <SubmissionsChart days={stats.submissionsByDay} />
          </Card>
        </>
      )}
    </div>
  );
}
