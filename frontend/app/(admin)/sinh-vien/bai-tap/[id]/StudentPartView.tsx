'use client';

import { Alert, Button, Collapse, Image, Tag } from 'antd';
import { Clock, FileText, History, Lock, PenLine } from 'lucide-react';
import Link from 'next/link';

import { BandBadge } from '@/components/BandBadge';
import { CountdownTimer } from '@/components/CountdownTimer';
import { StatusTag } from '@/components/StatusTag';
import { fileUrl, resultPath, TASK_TYPE_LABEL, writingPath } from '@/lib/constants';
import { getSubmissionDisplayStatus, SUBMISSION_STATUS_META } from '@/lib/theme/status';
import type { StudentPartDetail } from '@/lib/types/student';
import { formatDateTime } from '@/lib/utils/date';
import { formatScore } from '@/lib/utils/grading';

// ─── Một phần bài phía sinh viên: trạng thái, hạn, nút làm bài, đề, tiêu chí chấm ─
// `prompt` là HTML đã được server lọc — trường duy nhất được render bằng dangerouslySetInnerHTML.
// Được nộp hay không theo `canSubmit` của server, không tự so giờ.

function writeLabel(part: StudentPartDetail): string {
  if (part.status === 'REVISION_REQUESTED') return 'Sửa bài';
  return part.status === 'DRAFT' ? 'Viết tiếp' : 'Làm bài';
}

export function StudentPartView({
  assignmentId,
  assignmentClosed,
  part,
  band,
  paper = false,
  onHistory
}: {
  assignmentId: number;
  assignmentClosed: boolean;
  part: StudentPartDetail;
  /** Band của lần nộp gần nhất đã được trả; null = chưa có. */
  band: number | null;
  /** Bài PAPER: `band` là điểm riêng thang 10. */
  paper?: boolean;
  onHistory: () => void;
}) {
  const pending = part.status === 'NOT_STARTED' || part.status === 'DRAFT' || part.status === 'REVISION_REQUESTED';

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-col gap-2">
          <div className="flex flex-wrap items-center gap-2 text-small">
            <StatusTag meta={SUBMISSION_STATUS_META[getSubmissionDisplayStatus(part.status, part.late, 'student')]} />
            <Tag color="blue" className="m-0">
              {TASK_TYPE_LABEL[part.taskType]}
            </Tag>
            {part.minWords ? (
              <span className="inline-flex items-center gap-1 text-neutral-500">
                <FileText size={14} /> Tối thiểu {part.minWords} từ
              </span>
            ) : null}
            {part.suggestedMinutes ? (
              <span className="inline-flex items-center gap-1 text-neutral-500">
                <Clock size={14} /> Gợi ý {part.suggestedMinutes} phút
              </span>
            ) : null}
          </div>
          {part.effectiveDueAt && (
            <span className="inline-flex flex-wrap items-center gap-2 text-small text-neutral-500 tabular-nums">
              {part.status === 'REVISION_REQUESTED' ? 'Hạn sửa' : 'Hạn nộp'} {formatDateTime(part.effectiveDueAt)}
              {pending && !assignmentClosed && <CountdownTimer dueAt={part.effectiveDueAt} />}
            </span>
          )}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {band !== null && (
            <span className="inline-flex items-center gap-2 text-small text-neutral-500">
              {paper ? (
                <>
                  Điểm <span className="font-semibold text-neutral-900 tabular-nums">{formatScore(band)} / 10</span>
                </>
              ) : (
                <>
                  Band <BandBadge band={band} />
                </>
              )}
            </span>
          )}
          {band !== null && (
            <Link href={resultPath(assignmentId, part.partId)}>
              <Button type={pending ? 'default' : 'primary'}>Xem kết quả</Button>
            </Link>
          )}
          {part.currentVersion > 0 && (
            <Button icon={<History size={16} />} onClick={onHistory}>
              Lịch sử nộp
            </Button>
          )}
          {part.canSubmit && pending ? (
            <Link href={writingPath(assignmentId, part.partId)}>
              <Button type="primary" danger={part.status === 'REVISION_REQUESTED'} icon={<PenLine size={16} />}>
                {writeLabel(part)}
              </Button>
            </Link>
          ) : part.currentVersion > 0 ? (
            <Link href={writingPath(assignmentId, part.partId)}>
              <Button>Xem bài đã nộp</Button>
            </Link>
          ) : null}
        </div>
      </div>

      {pending && !part.canSubmit && (
        <Alert
          type="error"
          showIcon
          title={
            assignmentClosed
              ? 'Bài tập đã đóng, không nộp được nữa.'
              : 'Đã quá hạn nộp. Liên hệ giáo viên nếu cần gia hạn.'
          }
        />
      )}

      <section>
        <h4 className="mb-2 text-caption font-semibold tracking-wide text-neutral-500 uppercase">Đề bài</h4>
        <div
          className="rounded-md border border-neutral-100 bg-neutral-25 p-4 font-serif text-prompt text-neutral-900 [&_ol]:list-decimal [&_ol]:pl-5 [&_p]:mb-2 [&_ul]:list-disc [&_ul]:pl-5"
          dangerouslySetInnerHTML={{ __html: part.prompt }}
        />
        {part.imageUrl && (
          <Image
            src={fileUrl(part.imageUrl)}
            alt={`Hình minh hoạ đề: ${part.title}`}
            className="mt-3 max-h-80 rounded-md border border-neutral-100 object-contain"
          />
        )}
      </section>

      {part.criteria.length > 0 && (
        <section>
          <h4 className="mb-2 text-caption font-semibold tracking-wide text-neutral-500 uppercase">
            Tiêu chí chấm ({part.criteria.length})
          </h4>
          <Collapse
            size="small"
            items={part.criteria.map((c, index) => ({
              key: index,
              label: (
                <span className="font-medium text-neutral-900">
                  {c.code && <span className="mr-2 font-mono text-brand-600">{c.code}</span>}
                  {c.name}
                </span>
              ),
              extra: <span className="text-small text-neutral-500">Tối đa {formatScore(c.maxScore)}</span>,
              children: (
                <div className="flex flex-col gap-2">
                  {c.description && <p className="text-small text-neutral-500">{c.description}</p>}
                  {c.checklist.length === 0 ? (
                    <p className="text-small text-neutral-400">Không có checklist</p>
                  ) : (
                    <ul className="m-0 flex list-none flex-col gap-1.5 p-0">
                      {c.checklist.map((item, i) => (
                        <li key={i} className="flex items-start justify-between gap-3">
                          <span className="flex items-start gap-2">
                            <span
                              className={`mt-1.5 size-1.5 shrink-0 rounded-full ${item.kind === 'LIMIT' ? 'bg-warning' : 'bg-success'}`}
                            />
                            {item.content}
                          </span>
                          {item.kind === 'LIMIT' && item.maxBand !== null && (
                            <Tag color="warning" className="m-0 inline-flex shrink-0 items-center gap-1">
                              <Lock size={11} /> Tối đa {formatScore(item.maxBand)}
                            </Tag>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )
            }))}
          />
        </section>
      )}
    </div>
  );
}
