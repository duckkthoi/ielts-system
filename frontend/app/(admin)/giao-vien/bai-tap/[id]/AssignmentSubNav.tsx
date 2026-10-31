'use client';

import { useEffect, useState } from 'react';

import { Result, Skeleton } from 'antd';
import clsx from 'clsx';
import { ChevronRight } from 'lucide-react';
import Link from 'next/link';

import { assignmentApi } from '@/lib/api/assignment';
import type { AssignmentDetail, Targets } from '@/lib/types/assignment';

// ─── Khung chung của mọi trang thuộc một bài tập ──────────────────────────────
// Một chỗ điều hướng duy nhất trong bài: breadcrumb · tên bài · tab Chi tiết · Nhóm · Tiến độ · Giám sát · Thống kê.
// Sidebar chỉ giữ điều hướng cấp hệ thống; thao tác trên bài (giao thêm, cài đặt, đóng, nhân bản) nằm ở `extra`
// của trang Chi tiết. Hook tải bài + danh sách đã giao (để lọc theo lớp); bài nháp chưa có tiến độ nên coi như
// không tìm thấy ở các trang con.

export type AssignmentView = 'chi-tiet' | 'nhom' | 'tien-do' | 'giam-sat' | 'thong-ke';

const VIEW_OPTIONS: { value: AssignmentView; label: string }[] = [
  { value: 'chi-tiet', label: 'Chi tiết' },
  { value: 'nhom', label: 'Nhóm' },
  { value: 'tien-do', label: 'Tiến độ' },
  { value: 'giam-sat', label: 'Giám sát' },
  { value: 'thong-ke', label: 'Thống kê' }
];

const LIST_PATH = '/giao-vien/bai-tap';

export const assignmentViewPath = (id: number, view: AssignmentView) =>
  view === 'chi-tiet' ? `${LIST_PATH}/${id}` : `${LIST_PATH}/${id}/${view}`;

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

/** Tab của bài; `paper`: bài Paper có thêm tab "Nhóm" (Mức 4). Điện thoại: cuộn ngang, không tràn trang. */
export function AssignmentSubNav({ id, view, paper = false }: { id: number; view: AssignmentView; paper?: boolean }) {
  const options = VIEW_OPTIONS.filter((o) => paper || o.value !== 'nhom');
  return (
    <nav aria-label="Các trang của bài tập" className="-mx-4 mb-5 overflow-x-auto px-4 sm:mx-0 sm:px-0">
      <div className="flex min-w-max gap-1 border-b border-neutral-100">
        {options.map((o) => {
          const active = o.value === view;
          return (
            <Link
              key={o.value}
              href={assignmentViewPath(id, o.value)}
              aria-current={active ? 'page' : undefined}
              className={clsx(
                '-mb-px whitespace-nowrap border-b-2 px-3 py-2 transition-colors',
                active
                  ? 'border-brand-600 font-medium text-brand-700'
                  : 'border-transparent text-neutral-500 hover:text-neutral-900'
              )}
            >
              {o.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}

/**
 * Đầu trang của một bài: breadcrumb, tên bài (+ `meta` dưới tên), mô tả của trang, vùng nút `extra`, rồi tab.
 * `title` chưa có (đang tải) thì chỉ hiện khung. `tabs = false` cho bài nháp (chưa có trang con).
 */
export function AssignmentHeader({
  id,
  view,
  paper = false,
  title,
  meta,
  description,
  extra,
  tabs = true
}: {
  id: number;
  view: AssignmentView;
  paper?: boolean;
  title?: string;
  meta?: React.ReactNode;
  description?: React.ReactNode;
  extra?: React.ReactNode;
  tabs?: boolean;
}) {
  return (
    <>
      <nav aria-label="Breadcrumb" className="mb-2 flex min-w-0 items-center gap-1 text-small text-neutral-500">
        <Link href={LIST_PATH} className="shrink-0 hover:text-brand-600">
          Bài tập
        </Link>
        {title && (
          <>
            <ChevronRight size={14} className="shrink-0" />
            <span className="truncate text-neutral-700">{title}</span>
          </>
        )}
      </nav>
      {title && (
        <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
          <div className="min-w-0">
            <h1 className="text-h1 font-semibold break-words text-neutral-900">{title}</h1>
            {meta && <div className="mt-1 text-neutral-500">{meta}</div>}
            {description && <p className="mt-1 text-neutral-500">{description}</p>}
          </div>
          {extra && <div className="flex flex-wrap items-center gap-2">{extra}</div>}
        </div>
      )}
      {tabs && <AssignmentSubNav id={id} view={view} paper={paper} />}
    </>
  );
}

/** Trạng thái chờ / không tìm thấy dùng chung cho các trang con. */
export function AssignmentHeaderFallback({
  id,
  view,
  notFound,
  paper = false
}: {
  id: number;
  view: AssignmentView;
  notFound: boolean;
  paper?: boolean;
}) {
  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <AssignmentHeader id={id} view={view} paper={paper} tabs={!notFound} />
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
