'use client';

import { useEffect, useMemo, useState } from 'react';

import { Alert, Button, Card, Empty, Result, Select, Skeleton } from 'antd';
import { ArrowLeft } from 'lucide-react';
import Link from 'next/link';
import { useParams, useRouter, useSearchParams } from 'next/navigation';

import { BandBadge } from '@/components/BandBadge';
import { EssayAnnotator, studentAnnotationViews } from '@/components/EssayAnnotator';
import { PaperViewer } from '@/components/PaperEditor';
import { PageHeader } from '@/components/PageHeader';
import { resultPath, studentAssignmentPath, writingPath } from '@/lib/constants';
import { studentApi } from '@/lib/api/student';
import { getBandLevel } from '@/lib/theme/status';
import type { StudentAssignmentDetail, VersionDetail, VersionSummary } from '@/lib/types/student';
import { formatDate, formatDateTime } from '@/lib/utils/date';
import { formatScore } from '@/lib/utils/grading';

import { CriteriaResult } from './CriteriaResult';
import { StudentCompare } from './StudentCompare';

// ─── Sinh viên: kết quả một phần (UC-23, Prompt 7 D, E) ───────────────────────
// /sinh-vien/bai-tap/{id}/ket-qua?phan={partId}&lan={versionNo}; bỏ trống `lan` = lần gần nhất đã trả bài.
// Kết quả chỉ có khi lần nộp đã được trả (BR-13); đang "Cần sửa" thì server đã ẩn gợi ý sửa (BR-21).
// Bài PAPER (B25e): bản nộp có định dạng, điểm thang 10 — điểm riêng của mình (đã cộng / trừ) và điểm nhóm;
// đánh dấu của giáo viên tô trên bản có định dạng (B25d), bấm để xem nhận xét.

type Loaded = { detail: StudentAssignmentDetail; versions: VersionSummary[]; version: VersionDetail | null };

const num = (v: string | null) => (v && /^\d+$/.test(v) ? Number(v) : undefined);

export default function StudentResultPage() {
  const router = useRouter();
  const id = Number(useParams<{ id: string }>().id);
  const sp = useSearchParams();
  const partId = num(sp.get('phan'));
  const lan = num(sp.get('lan'));
  const [loaded, setLoaded] = useState<{ key: string; data: Loaded } | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [selected, setSelected] = useState<string | null>(null);
  const key = `${id}-${partId}-${lan}`;

  useEffect(() => {
    if (!partId) return;
    let cancelled = false;
    (async () => {
      const detail = await studentApi.assignment(id);
      if (!detail.parts.some((p) => p.partId === partId)) throw new Error('part');
      const versions = await studentApi.versions(partId);
      const no = lan ?? versions.find((v) => v.returned)?.versionNo ?? versions[0]?.versionNo;
      const version = no ? await studentApi.version(partId, no) : null;
      return { detail, versions, version };
    })()
      .then((data) => {
        if (!cancelled) setLoaded({ key, data });
      })
      .catch(() => {
        if (!cancelled) setNotFound(true);
      });
    return () => {
      cancelled = true;
    };
  }, [id, partId, lan, key]);

  const data = loaded?.key === key ? loaded.data : null;
  const views = useMemo(
    () => (data?.version?.result ? studentAnnotationViews(data.version.result.inlineComments) : []),
    [data]
  );

  const back = (
    <Link
      href={studentAssignmentPath(id)}
      className="mb-3 inline-flex items-center gap-1 text-small text-neutral-500 hover:text-brand-600"
    >
      <ArrowLeft size={14} /> Chi tiết bài
    </Link>
  );

  if (notFound || !partId) {
    return (
      <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
        {back}
        <Result
          status="404"
          title="Không tìm thấy kết quả"
          subTitle="Phần bài không tồn tại hoặc không được giao cho bạn."
        />
      </div>
    );
  }
  if (!data) {
    return (
      <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
        {back}
        <Skeleton active paragraph={{ rows: 10 }} />
      </div>
    );
  }

  const { detail, versions, version } = data;
  const part = detail.parts.find((p) => p.partId === partId)!;
  const result = version?.result ?? null;
  const band = result?.band ?? null;
  const paper = detail.kind === 'PAPER';
  const myScore = result?.myScore ?? null;

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      {back}
      <PageHeader
        title={`Kết quả · ${part.title}`}
        description={
          version
            ? `${detail.title} · Lần nộp ${version.versionNo}${
                result?.returnedAt ? ` · Đã trả bài lúc ${formatDateTime(result.returnedAt)}` : ''
              }`
            : detail.title
        }
        extra={
          <span className="flex flex-wrap items-center gap-3">
            {paper && myScore !== null && (
              <span className="inline-flex items-baseline gap-2">
                <span className="text-h2 font-semibold text-brand-600 tabular-nums">{formatScore(myScore)}</span>
                <span className="text-small text-neutral-500">
                  / 10{band !== null && myScore !== band ? ` · điểm nhóm ${formatScore(band)}` : ''}
                </span>
              </span>
            )}
            {!paper && band !== null && (
              <span className="inline-flex items-center gap-2">
                <BandBadge band={band} size="lg" />
                <span className="font-semibold text-neutral-900">{getBandLevel(band).label}</span>
              </span>
            )}
            {versions.length > 1 && version && (
              <Select
                className="min-w-48"
                value={version.versionNo}
                onChange={(v: number) => router.replace(resultPath(id, partId, v), { scroll: false })}
                options={versions.map((v) => ({
                  value: v.versionNo,
                  label: (
                    <span className="inline-flex items-center gap-2">
                      Lần {v.versionNo} · {formatDate(v.submittedAt)}
                      {!v.returned ? (
                        ' · chờ kết quả'
                      ) : paper ? (
                        <span className="font-semibold tabular-nums">
                          {v.band === null ? '—' : formatScore(v.band)} / 10
                        </span>
                      ) : (
                        <BandBadge band={v.band} size="sm" />
                      )}
                    </span>
                  )
                }))}
              />
            )}
          </span>
        }
      />

      {part.status === 'REVISION_REQUESTED' && version?.versionNo === part.currentVersion && (
        <Alert
          type="error"
          showIcon
          className="mb-4"
          title={`Giáo viên yêu cầu sửa bài${part.revisionDueAt ? ` · Hạn sửa ${formatDateTime(part.revisionDueAt)}` : ''}`}
          description={part.revisionNote ? `Lời nhắn: ${part.revisionNote}` : undefined}
          action={
            part.canSubmit ? (
              <Link href={writingPath(id, part.partId)}>
                <Button type="primary" danger>
                  Sửa bài
                </Button>
              </Link>
            ) : undefined
          }
        />
      )}
      {result?.myAdjustment != null && (
        <Alert
          type={result.myAdjustment > 0 ? 'success' : 'warning'}
          showIcon
          className="mb-4"
          title={`Giáo viên ${result.myAdjustment > 0 ? 'cộng' : 'trừ'} ${formatScore(Math.abs(result.myAdjustment))} điểm riêng cho bạn`}
          description={result.myAdjustmentReason ? `Lý do: ${result.myAdjustmentReason}` : undefined}
        />
      )}
      {result?.suggestionsHidden && (
        <Alert
          type="info"
          showIcon
          className="mb-4"
          title="Gợi ý sửa sẽ hiện sau khi bạn nộp bản sửa. Hãy dựa vào mã lỗi và nhận xét để tự sửa."
        />
      )}

      {!version ? (
        <Empty description="Bạn chưa nộp phần này." />
      ) : !result ? (
        <Card>
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={`Lần nộp ${version.versionNo} đang chờ giáo viên chấm. Kết quả hiện ở đây khi giáo viên trả bài.`}
          />
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1fr)_380px]">
          <div className="flex min-w-0 flex-col gap-4">
            {paper && version.contentJson ? (
              <PaperViewer
                contentJson={version.contentJson}
                references={version.references ?? []}
                style={detail.citationStyle ?? 'IEEE'}
                footer={<span className="text-caption text-neutral-500">{version.wordCount} từ</span>}
                annotate={{
                  content: version.content,
                  annotations: views,
                  selectedKey: selected,
                  onSelect: setSelected
                }}
              />
            ) : (
              <EssayAnnotator
                content={version.content}
                annotations={views}
                readOnly
                selectedKey={selected}
                onSelect={setSelected}
              />
            )}
            {!paper && (
              <Card size="small" title="Lỗi trong bài">
                {result.errorSummary.length === 0 ? (
                  <p className="text-small text-neutral-500">Giáo viên không gắn mã lỗi nào.</p>
                ) : (
                  <ul className="m-0 flex list-none flex-col gap-1.5 p-0 text-small">
                    {result.errorSummary.map((e) => (
                      <li key={`${e.criterionCode}-${e.errorCode}`} className="flex items-center justify-between gap-3">
                        <span className="min-w-0 truncate">
                          <span className="font-mono font-semibold text-neutral-900">{e.errorCode}</span> {e.errorName}
                          {e.criterionCode && <span className="text-neutral-500"> · {e.criterionCode}</span>}
                        </span>
                        <strong className="shrink-0 tabular-nums">×{e.count}</strong>
                      </li>
                    ))}
                  </ul>
                )}
              </Card>
            )}
          </div>
          <CriteriaResult result={result} />
        </div>
      )}

      {version && versions.some((v) => v.versionNo < version.versionNo) && (
        <Card title="So sánh các lần nộp" className="mt-4">
          <StudentCompare key={version.versionNo} partId={partId} versions={versions} current={version} />
        </Card>
      )}
    </div>
  );
}
