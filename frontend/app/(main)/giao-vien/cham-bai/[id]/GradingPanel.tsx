'use client';

import { Alert, Button, Input, Tooltip } from 'antd';
import clsx from 'clsx';
import { AlertTriangle, Check, Loader2, Pencil, Send } from 'lucide-react';

import { BandBadge } from '@/components/BandBadge';
import { ZERO_REASON_LABEL } from '@/lib/constants';
import type { GradingScreen } from '@/lib/types/grading';
import { formatDateTime, formatTime } from '@/lib/utils/date';

import { CriterionCard } from './CriterionCard';
import { capOf, useGradeForm } from './useGradeForm';

// ─── Bảng chấm bên phải: tiêu chí → band phần → nhận xét chung → thanh hành động dính đáy ─
// Nút chỉ để dễ dùng; server kiểm tra lại mọi điều kiện (chấm đủ, còn lượt sửa, bài chưa đóng — BR-10, UC-18).

type GradeFormApi = ReturnType<typeof useGradeForm>;

function SaveStatus({ g }: { g: GradeFormApi }) {
  const base = 'inline-flex items-center gap-1 text-caption';
  if (g.state === 'SAVING')
    return (
      <span className={clsx(base, 'text-neutral-500')}>
        <Loader2 size={12} className="animate-spin" /> Đang lưu…
      </span>
    );
  if (g.state === 'DIRTY') return <span className={clsx(base, 'text-neutral-500')}>Chưa lưu</span>;
  if (g.state === 'ERROR')
    return (
      <button
        type="button"
        onClick={() => void g.save()}
        title={g.error ?? undefined}
        className={clsx(base, 'cursor-pointer border-0 bg-transparent p-0 text-error hover:underline')}
      >
        <AlertTriangle size={12} /> Lỗi lưu – thử lại
      </button>
    );
  return (
    <span className={clsx(base, 'text-neutral-500')}>
      <Check size={12} className="text-success" />
      {g.grade ? `Đã lưu lúc ${formatTime(g.grade.updatedAt)}` : 'Chưa chấm'}
    </span>
  );
}

export function GradingPanel({
  screen,
  g,
  busy,
  onZero,
  onReturn,
  onRevision
}: {
  screen: GradingScreen;
  g: GradeFormApi;
  busy: boolean;
  onZero: () => void;
  onReturn: () => void;
  onRevision: () => void;
}) {
  const { editable, assignment, submission } = screen;
  const zero = g.form.zeroReason;
  const revisionsLeft = assignment.maxRevisions - submission.revisionsUsed;
  const reviseBlock = !g.allScored
    ? 'Chấm đủ mọi tiêu chí trước'
    : revisionsLeft <= 0
      ? 'Đã hết lượt yêu cầu sửa'
      : assignment.status === 'CLOSED'
        ? 'Bài tập đã đóng, sinh viên không nộp lại được'
        : null;

  return (
    <div className="flex min-h-full flex-col">
      <div className="flex flex-col gap-3 p-4 sm:p-5">
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-h2 font-semibold text-neutral-900">Chấm điểm</h2>
          {editable && !zero && (
            <Button type="text" danger onClick={onZero}>
              Chấm 0
            </Button>
          )}
        </div>

        {!editable && (
          <Alert
            type="info"
            showIcon
            title={
              g.grade?.returnedAt
                ? `Đã trả bài lúc ${formatDateTime(g.grade.returnedAt)}${g.grade.graderName ? ` · ${g.grade.graderName}` : ''}`
                : 'Đây là lần nộp cũ — chỉ xem, không chấm lại được'
            }
          />
        )}

        {zero && (
          <Alert
            type="error"
            showIcon
            title={`Chấm 0 cả bài: ${ZERO_REASON_LABEL[zero]}`}
            action={
              editable ? (
                <Button size="small" onClick={() => g.setZeroReason(null)}>
                  Bỏ chấm 0
                </Button>
              ) : undefined
            }
          />
        )}

        {screen.criteria.map((c) => (
          <CriterionCard
            key={c.id}
            criterion={c}
            score={zero ? 0 : g.form.scores[c.id]}
            comment={g.form.comments[c.id] ?? ''}
            checked={g.form.checked}
            cap={capOf(c, g.form.checked, screen.version.wordCount)}
            disabled={!editable || zero !== null}
            onScore={(s) => g.setScore(c.id, s)}
            onComment={(v) => g.setComment(c.id, v)}
            onToggle={(itemId, v) => g.toggleItem(c, itemId, v)}
          />
        ))}

        <div className="flex items-center gap-4 rounded-lg border border-neutral-100 bg-neutral-0 p-4">
          <BandBadge band={g.bandPreview} size="lg" />
          <div>
            <p className="font-semibold text-neutral-900">Band của phần</p>
            <p className="text-small text-neutral-500">
              {g.bandPreview === null
                ? 'Chấm đủ mọi tiêu chí để có band'
                : 'Trung bình có trọng số các tiêu chí, làm tròn theo IELTS (.25 / .75)'}
            </p>
          </div>
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="general-comment" className="font-semibold text-neutral-900">
            Nhận xét chung
          </label>
          <Input.TextArea
            id="general-comment"
            value={g.form.generalComment}
            onChange={(e) => g.setGeneralComment(e.target.value)}
            disabled={!editable}
            maxLength={5000}
            autoSize={{ minRows: 3, maxRows: 10 }}
            placeholder="Điểm mạnh, điểm cần cải thiện…"
          />
        </div>
      </div>

      {editable && (
        <div className="sticky bottom-0 mt-auto flex flex-wrap items-center gap-2 border-t border-neutral-100 bg-neutral-0 px-4 py-3 sm:px-5">
          <span className="mr-auto flex flex-col">
            <Button onClick={() => void g.save()} disabled={busy}>
              Lưu nháp
            </Button>
          </span>
          <SaveStatus g={g} />
          <Tooltip title={reviseBlock ?? `Còn ${revisionsLeft} lần yêu cầu sửa`}>
            <Button icon={<Pencil size={16} />} disabled={busy || reviseBlock !== null} onClick={onRevision}>
              Yêu cầu sửa
            </Button>
          </Tooltip>
          <Tooltip title={g.allScored ? undefined : 'Chấm đủ mọi tiêu chí trước khi trả bài'}>
            <Button type="primary" icon={<Send size={16} />} loading={busy} disabled={!g.allScored} onClick={onReturn}>
              Trả bài
            </Button>
          </Tooltip>
        </div>
      )}
    </div>
  );
}
