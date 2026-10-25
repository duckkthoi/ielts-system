'use client';

import { useEffect, useState } from 'react';

import { Alert, Button, Card, Descriptions, Result, Skeleton, Tabs } from 'antd';
import { ArrowLeft } from 'lucide-react';
import Link from 'next/link';
import { useParams, useSearchParams } from 'next/navigation';

import { BandBadge } from '@/components/BandBadge';
import { CountdownTimer } from '@/components/CountdownTimer';
import { PageHeader } from '@/components/PageHeader';
import { StatusTag } from '@/components/StatusTag';
import { writingPath } from '@/lib/constants';
import { studentApi } from '@/lib/api/student';
import { ASSIGNMENT_STATUS_META, getBandLevel } from '@/lib/theme/status';
import type { AssignmentResult, StudentAssignmentDetail, StudentPartDetail } from '@/lib/types/student';
import { formatDateTime } from '@/lib/utils/date';
import { nextStudentAction } from '@/lib/utils/studentAction';

import { StudentPartView } from './StudentPartView';
import { VersionHistoryDrawer } from './VersionHistoryDrawer';

// ─── Sinh viên: chi tiết bài được giao (UC-22, Prompt 7 B) ────────────────────
// Thông báo "bài mới / đã trả bài / cần sửa / sắp hết hạn" đều trỏ tới trang này.
// Không được giao / chưa tới giờ mở → server trả 404 (BR-04, BR-05).

const LIST_PATH = '/sinh-vien/tong-quan';

export default function StudentAssignmentDetailPage() {
  const id = Number(useParams<{ id: string }>().id);
  const partParam = Number(useSearchParams().get('phan'));
  const [loaded, setLoaded] = useState<{ detail: StudentAssignmentDetail; result: AssignmentResult } | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [historyPart, setHistoryPart] = useState<StudentPartDetail | null>(null);

  useEffect(() => {
    let cancelled = false;
    studentApi
      .assignment(id)
      .then(async (detail) => {
        const result = await studentApi.result(id);
        if (!cancelled) setLoaded({ detail, result });
      })
      .catch(() => {
        if (!cancelled) setNotFound(true);
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  const back = (
    <Link
      href={LIST_PATH}
      className="mb-3 inline-flex items-center gap-1 text-small text-neutral-500 hover:text-brand-600"
    >
      <ArrowLeft size={14} /> Bài được giao
    </Link>
  );

  if (notFound) {
    return (
      <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
        {back}
        <Result
          status="404"
          title="Không tìm thấy bài tập"
          subTitle="Bài tập không tồn tại, chưa tới giờ mở hoặc không được giao cho bạn."
        />
      </div>
    );
  }

  if (!loaded) {
    return (
      <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
        {back}
        <Skeleton active paragraph={{ rows: 8 }} />
      </div>
    );
  }

  const { detail, result } = loaded;
  const closed = detail.status === 'CLOSED';
  const bandOf = (partId: number) => result.parts.find((p) => p.partId === partId)?.band ?? null;
  const revisions = detail.parts.filter((p) => p.status === 'REVISION_REQUESTED');
  const activePart =
    detail.parts.find((p) => p.partId === partParam)?.partId ??
    nextStudentAction(detail.parts).partId ??
    detail.parts[0]?.partId;
  const hasBand = result.parts.some((p) => p.band !== null);

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      {back}
      <PageHeader
        title={detail.title}
        description={
          <span className="inline-flex flex-wrap items-center gap-2">
            {detail.teacherName} · {detail.parts.length} phần
            {closed && <StatusTag meta={ASSIGNMENT_STATUS_META.CLOSED} size="sm" />}
          </span>
        }
        extra={
          result.complete && result.band !== null ? (
            <span className="inline-flex items-center gap-2">
              <BandBadge band={result.band} size="lg" />
              <span className="font-semibold text-neutral-900">{getBandLevel(result.band).label}</span>
            </span>
          ) : undefined
        }
      />

      {revisions.map((p) => (
        <Alert
          key={p.partId}
          type="error"
          showIcon
          className="mb-4"
          title={
            <>
              Giáo viên yêu cầu sửa {p.title}
              {p.revisionDueAt && <> · Hạn sửa {formatDateTime(p.revisionDueAt)}</>}
            </>
          }
          description={p.revisionNote ? `Lời nhắn: ${p.revisionNote}` : undefined}
          action={
            p.canSubmit ? (
              <Link href={writingPath(detail.id, p.partId)}>
                <Button type="primary" danger>
                  Sửa bài
                </Button>
              </Link>
            ) : undefined
          }
        />
      ))}

      {detail.description && (
        <p className="mb-5 max-w-3xl whitespace-pre-line text-neutral-700">{detail.description}</p>
      )}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
        <Card title="Các phần" styles={{ body: { paddingTop: 4 } }}>
          <Tabs
            defaultActiveKey={activePart ? String(activePart) : undefined}
            items={detail.parts.map((part) => ({
              key: String(part.partId),
              label: part.title,
              children: (
                <StudentPartView
                  assignmentId={detail.id}
                  assignmentClosed={closed}
                  part={part}
                  band={bandOf(part.partId)}
                  onHistory={() => setHistoryPart(part)}
                />
              )
            }))}
          />
        </Card>

        <div className="flex flex-col gap-4">
          <Card title="Kết quả">
            {!hasBand ? (
              <p className="text-small text-neutral-500">Chưa có kết quả. Giáo viên trả bài thì band hiện ở đây.</p>
            ) : (
              <div className="flex flex-col gap-3">
                <ul className="m-0 flex list-none flex-col gap-2 p-0">
                  {result.parts.map((p) => (
                    <li key={p.partId} className="flex items-center justify-between gap-3">
                      <span className="truncate text-neutral-700">{p.title}</span>
                      <BandBadge band={p.band} size="sm" />
                    </li>
                  ))}
                </ul>
                <div className="flex items-center justify-between gap-3 border-t border-neutral-100 pt-3">
                  <span className="font-semibold text-neutral-900">Band cả bài</span>
                  <BandBadge band={result.band} />
                </div>
                {result.note && <p className="text-caption text-neutral-500">{result.note}</p>}
              </div>
            )}
          </Card>

          <Card title="Thời gian & quy định">
            <Descriptions
              column={1}
              size="small"
              items={[
                ...(detail.openAt ? [{ key: 'open', label: 'Mở lúc', children: formatDateTime(detail.openAt) }] : []),
                {
                  key: 'due',
                  label: 'Hạn nộp',
                  children: detail.dueAt ? (
                    <span className="flex flex-col items-start gap-1">
                      {formatDateTime(detail.dueAt)}
                      {!closed && <CountdownTimer dueAt={detail.dueAt} />}
                    </span>
                  ) : (
                    'Không có'
                  )
                },
                { key: 'late', label: 'Nộp muộn', children: detail.allowLate ? 'Được phép' : 'Không được phép' },
                { key: 'rev', label: 'Yêu cầu sửa', children: `Tối đa ${detail.maxRevisions} lần` }
              ]}
            />
          </Card>
        </div>
      </div>

      <VersionHistoryDrawer assignmentId={detail.id} part={historyPart} onClose={() => setHistoryPart(null)} />
    </div>
  );
}
