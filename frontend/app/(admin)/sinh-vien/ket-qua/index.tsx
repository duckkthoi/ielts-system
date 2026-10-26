'use client';

import { useEffect, useState } from 'react';

import { Button, Card, Empty, Skeleton } from 'antd';

import { PageHeader } from '@/components/PageHeader';
import { StudentResults } from '@/components/StudentResults';
import { statsApi } from '@/lib/api/stats';
import { resultPath, studentAssignmentPath } from '@/lib/constants';
import type { StudentStats } from '@/lib/types/stats';

// ─── Sinh viên: kết quả học tập (UC-28, Prompt 8) ─────────────────────────────

export default function StudentResultsPage() {
  const [stats, setStats] = useState<StudentStats | null>(null);
  const [failed, setFailed] = useState(false);

  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    statsApi
      .studentResults()
      .then((res) => {
        if (!cancelled) setStats(res);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  const retry = () => {
    setFailed(false);
    setReloadKey((k) => k + 1);
  };

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader title="Kết quả học tập" description="Theo dõi tiến bộ qua các bài đã được trả kết quả." />
      {failed ? (
        <Card>
          <Empty description="Không tải được kết quả học tập.">
            <Button onClick={retry}>Thử lại</Button>
          </Empty>
        </Card>
      ) : !stats ? (
        <div className="grid gap-4 sm:grid-cols-3">
          {Array.from({ length: 3 }, (_, i) => (
            <Card key={i}>
              <Skeleton active paragraph={{ rows: 1 }} />
            </Card>
          ))}
        </div>
      ) : (
        <StudentResults
          stats={stats}
          assignmentHref={studentAssignmentPath}
          partHref={(p) => resultPath(p.assignmentId, p.partId)}
        />
      )}
    </div>
  );
}
