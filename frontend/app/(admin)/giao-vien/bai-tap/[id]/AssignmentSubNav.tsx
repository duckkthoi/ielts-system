'use client';

import { useEffect, useState } from 'react';

import { Result, Segmented, Skeleton } from 'antd';
import { ArrowLeft } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import { assignmentApi } from '@/lib/api/assignment';
import type { AssignmentDetail, Targets } from '@/lib/types/assignment';

// ─── Khung chung của trang Tiến độ / Thống kê một bài (Prompt 5) ──────────────
// Link quay lại + chuyển nhanh Chi tiết · Tiến độ · Thống kê. Hook tải bài + danh sách đã giao
// (để lọc theo lớp); bài nháp chưa có tiến độ nên coi như không tìm thấy.

export type AssignmentView = 'chi-tiet' | 'tien-do' | 'thong-ke';

const VIEW_OPTIONS: { value: AssignmentView; label: string }[] = [
  { value: 'chi-tiet', label: 'Chi tiết' },
  { value: 'tien-do', label: 'Tiến độ' },
  { value: 'thong-ke', label: 'Thống kê' }
];

export const assignmentViewPath = (id: number, view: AssignmentView) =>
  view === 'chi-tiet' ? `/giao-vien/bai-tap/${id}` : `/giao-vien/bai-tap/${id}/${view}`;

export function useAssignmentHeader(id: number) {
  const [state, setState] = useState<{ assignment: AssignmentDetail; targets: Targets } | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    let cancelled = false;
    assignmentApi
      .detail(id)
      .then(async (detail) => {
        if (detail.status === 'DRAFT') throw new Error('draft');
        const targets = await assignmentApi.getTargets(id);
        if (!cancelled) setState({ assignment: detail, targets });
      })
      .catch(() => {
        if (!cancelled) setNotFound(true);
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  return { assignment: state?.assignment ?? null, targets: state?.targets ?? null, notFound };
}

export function AssignmentSubNav({ id, view }: { id: number; view: AssignmentView }) {
  const router = useRouter();
  return (
    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
      <Link
        href="/giao-vien/bai-tap"
        className="inline-flex items-center gap-1 text-small text-neutral-500 hover:text-brand-600"
      >
        <ArrowLeft size={14} /> Bài tập
      </Link>
      <Segmented<AssignmentView>
        value={view}
        options={VIEW_OPTIONS}
        onChange={(next) => router.push(assignmentViewPath(id, next))}
      />
    </div>
  );
}

/** Trạng thái chờ / không tìm thấy dùng chung cho hai trang. */
export function AssignmentHeaderFallback({ id, view, notFound }: { id: number; view: AssignmentView; notFound: boolean }) {
  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <AssignmentSubNav id={id} view={view} />
      {notFound ? (
        <Result
          status="404"
          title="Không có dữ liệu"
          subTitle="Bài tập không tồn tại, không phải của bạn hoặc chưa phát hành."
        />
      ) : (
        <Skeleton active paragraph={{ rows: 8 }} />
      )}
    </div>
  );
}
