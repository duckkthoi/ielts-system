import { Button, Card } from 'antd';
import { AlertCircle, ChevronRight } from 'lucide-react';
import Link from 'next/link';

import { CountdownTimer } from '@/components/CountdownTimer';
import { StatusTag } from '@/components/StatusTag';
import { studentAssignmentPath, writingPath } from '@/lib/constants';
import { ASSIGNMENT_STATUS_META, getSubmissionDisplayStatus, SUBMISSION_STATUS_META } from '@/lib/theme/status';
import type { StudentAssignmentItem } from '@/lib/types/student';
import { formatDateTime } from '@/lib/utils/date';
import { nextStudentAction } from '@/lib/utils/studentAction';

import { ACTION_LABEL } from './const';

// ─── Thẻ một bài được giao: các phần + trạng thái, hạn gần nhất, nút chính ────

export function AssignmentCard({ item }: { item: StudentAssignmentItem }) {
  const action = nextStudentAction(item.parts);
  const href =
    action.kind === 'write' || action.kind === 'revise'
      ? writingPath(item.id, action.partId)
      : studentAssignmentPath(item.id);
  const revisions = item.parts.filter((p) => p.status === 'REVISION_REQUESTED');

  return (
    <Card className="h-full transition-shadow hover:shadow-md" styles={{ body: { padding: 20, height: '100%' } }}>
      <div className="flex h-full flex-col gap-3">
        <div>
          <p className="text-small text-neutral-500">{item.teacherName}</p>
          <Link href={studentAssignmentPath(item.id)} className="block text-neutral-900 hover:text-brand-600">
            <h3 className="text-h3 font-semibold">{item.title}</h3>
          </Link>
          {item.status === 'CLOSED' && (
            <span className="mt-1 inline-block">
              <StatusTag meta={ASSIGNMENT_STATUS_META.CLOSED} size="sm" />
            </span>
          )}
        </div>

        <ul className="m-0 flex list-none flex-col gap-1.5 p-0">
          {item.parts.map((p) => (
            <li key={p.partId} className="flex items-center gap-2">
              <StatusTag
                meta={SUBMISSION_STATUS_META[getSubmissionDisplayStatus(p.status, p.late, 'student')]}
                size="sm"
              />
              <span className="truncate text-neutral-700">{p.title}</span>
            </li>
          ))}
        </ul>

        {revisions.map((p) => (
          <div
            key={p.partId}
            className="flex items-start gap-2 rounded-md border border-error-border bg-error-bg px-3 py-2 text-small text-error"
          >
            <AlertCircle size={16} className="mt-0.5 shrink-0" />
            <span>
              Giáo viên yêu cầu sửa <strong>{p.title}</strong>
              {p.effectiveDueAt && <> · Hạn sửa {formatDateTime(p.effectiveDueAt)}</>}
            </span>
          </div>
        ))}

        <div className="mt-auto flex flex-wrap items-center justify-between gap-2 pt-1">
          <span className="flex flex-col items-start gap-1 text-small text-neutral-500">
            {item.nextDueAt ? (
              <>
                <span className="tabular-nums">Hạn {formatDateTime(item.nextDueAt)}</span>
                <CountdownTimer dueAt={item.nextDueAt} />
              </>
            ) : item.dueAt ? (
              <span className="tabular-nums">Hạn {formatDateTime(item.dueAt)}</span>
            ) : null}
          </span>
          <Link href={href}>
            <Button type={action.kind === 'view' ? 'default' : 'primary'} danger={action.kind === 'revise'}>
              {ACTION_LABEL[action.kind]}
              {action.kind === 'view' && <ChevronRight size={14} />}
            </Button>
          </Link>
        </div>
      </div>
    </Card>
  );
}
