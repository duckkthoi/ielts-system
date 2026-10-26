'use client';

import { useCallback, useEffect, useState } from 'react';

import { Result, Skeleton, Tabs } from 'antd';
import Link from 'next/link';
import { useParams, usePathname, useRouter, useSearchParams } from 'next/navigation';

import { studentApi } from '@/lib/api/student';
import { timeApi } from '@/lib/api/time';
import { studentAssignmentPath } from '@/lib/constants';
import type { StudentAssignmentDetail } from '@/lib/types/student';
import { nextStudentAction } from '@/lib/utils/studentAction';

import { CLOCK_SYNC_MS, partTabLabel } from './const';
import { WritingPanel } from './WritingPanel';

// ─── Sinh viên: màn làm bài (UC-23, UC-24, Prompt 7 C) ────────────────────────
// /sinh-vien/lam-bai/{assignmentId}?phan={partId}. Đổi phần thì panel cũ tự lưu nốt nháp.
// Đồng hồ đếm ngược theo giờ server (SD-11): đồng bộ khi vào màn và mỗi 5 phút.

export default function WritingPage() {
  const router = useRouter();
  const pathname = usePathname();
  const id = Number(useParams<{ id: string }>().id);
  const partParam = Number(useSearchParams().get('phan'));
  const [detail, setDetail] = useState<StudentAssignmentDetail | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [version, setVersion] = useState(0);

  const reload = useCallback(() => setVersion((v) => v + 1), []);

  useEffect(() => {
    let cancelled = false;
    studentApi
      .assignment(id)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch(() => {
        if (!cancelled) setNotFound(true);
      });
    return () => {
      cancelled = true;
    };
  }, [id, version]);

  useEffect(() => {
    void timeApi.syncClock();
    const timer = setInterval(() => void timeApi.syncClock(), CLOCK_SYNC_MS);
    return () => clearInterval(timer);
  }, []);

  if (notFound) {
    return (
      <Result
        status="404"
        title="Không tìm thấy bài tập"
        subTitle="Bài tập không tồn tại, chưa tới giờ mở hoặc không được giao cho bạn."
        extra={<Link href="/sinh-vien/tong-quan">Về Bài được giao</Link>}
      />
    );
  }

  if (!detail) {
    return (
      <div className="mx-auto w-full max-w-180 p-6">
        <Skeleton active paragraph={{ rows: 12 }} />
      </div>
    );
  }

  const part =
    detail.parts.find((p) => p.partId === partParam) ??
    detail.parts.find((p) => p.partId === nextStudentAction(detail.parts).partId) ??
    detail.parts[0];

  return (
    <div className="flex flex-col">
      <div className="border-b border-neutral-100 bg-neutral-0 px-4 pt-3 sm:px-6">
        <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
          <h1 className="text-h3 font-semibold text-neutral-900">{detail.title}</h1>
          <Link href={studentAssignmentPath(detail.id)} className="text-small text-brand-600 hover:text-brand-700">
            Chi tiết bài &amp; lịch sử nộp
          </Link>
        </div>
        {detail.parts.length > 1 && (
          <Tabs
            className="-mb-px"
            activeKey={part ? String(part.partId) : undefined}
            onChange={(key) => router.replace(`${pathname}?phan=${key}`, { scroll: false })}
            items={detail.parts.map((p) => ({ key: String(p.partId), label: partTabLabel(p) }))}
          />
        )}
        {detail.parts.length <= 1 && <div className="h-3" />}
      </div>

      {part ? (
        <WritingPanel
          key={`${part.partId}-${part.status}-${part.currentVersion}-${part.canSubmit}`}
          assignmentId={detail.id}
          assignmentClosed={detail.status === 'CLOSED'}
          part={part}
          onChanged={reload}
        />
      ) : (
        <Result status="info" title="Bài tập chưa có phần nào" />
      )}
    </div>
  );
}
