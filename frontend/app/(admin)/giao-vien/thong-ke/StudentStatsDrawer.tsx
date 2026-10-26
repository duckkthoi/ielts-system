'use client';

import { useEffect, useState } from 'react';

import { Empty, Skeleton } from 'antd';

import { AppDrawer } from '@/components/AppDrawer';
import { StudentResults } from '@/components/StudentResults';
import { statsApi } from '@/lib/api/stats';
import type { StudentRef, StudentStats } from '@/lib/types/stats';

// ─── Kết quả học tập của một sinh viên trên các bài của tôi (FR-64) ───────────
// Cùng giao diện với trang "Kết quả học tập" của sinh viên, không có link sang trang của sinh viên.

export function StudentStatsDrawer({ student, onClose }: { student: StudentRef | null; onClose: () => void }) {
  const [result, setResult] = useState<{ id: number; stats: StudentStats | null } | null>(null);

  useEffect(() => {
    if (!student) return;
    let cancelled = false;
    statsApi
      .studentStats(student.id)
      .then((stats) => {
        if (!cancelled) setResult({ id: student.id, stats });
      })
      .catch(() => {
        if (!cancelled) setResult({ id: student.id, stats: null });
      });
    return () => {
      cancelled = true;
    };
  }, [student]);

  const current = student && result?.id === student.id ? result : null;

  return (
    <AppDrawer
      open={student !== null}
      onClose={onClose}
      size={960}
      title={student?.fullName}
      subtitle={student ? `${student.studentCode} · kết quả trên các bài của bạn` : undefined}
    >
      {!current ? (
        <Skeleton active paragraph={{ rows: 10 }} />
      ) : current.stats ? (
        <StudentResults stats={current.stats} />
      ) : (
        <Empty description="Không tải được kết quả của sinh viên này." />
      )}
    </AppDrawer>
  );
}
