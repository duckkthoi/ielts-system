import { Alert, Card } from 'antd';
import clsx from 'clsx';
import { Check } from 'lucide-react';

import { BandBadge } from '@/components/BandBadge';
import { ZERO_REASON_LABEL } from '@/lib/constants';
import type { VersionResult } from '@/lib/types/student';
import { formatScore } from '@/lib/utils/grading';

// ─── Cột phải màn kết quả: điểm từng tiêu chí, checklist (đạt / chưa đạt), giới hạn điểm, nhận xét chung ─

export function CriteriaResult({ result }: { result: VersionResult }) {
  return (
    <div className="flex flex-col gap-3">
      {result.zeroReason && (
        <Alert type="error" showIcon title={`Bài được chấm 0: ${ZERO_REASON_LABEL[result.zeroReason]}`} />
      )}

      {result.criteria.some((c) => c.checklist.length > 0) && (
        <p className="flex flex-wrap items-center gap-3 text-caption text-neutral-500">
          <span className="inline-flex items-center gap-1">
            <span className="inline-flex size-3 rounded-full bg-success" /> Đạt
          </span>
          <span className="inline-flex items-center gap-1">
            <span className="inline-flex size-3 rounded-full border border-neutral-300" /> Chưa đạt
          </span>
          <span className="inline-flex items-center gap-1">
            <span className="inline-flex size-3 rounded-full bg-warning" /> Điểm yếu làm giới hạn điểm
          </span>
        </p>
      )}

      {result.criteria.map((c) => {
        // "Giới hạn điểm" chỉ hiện khi bị tick — mục không tick không phải điểm yếu
        const marks = c.checklist.filter((item) => item.kind === 'CHECK' || item.checked);
        return (
          <Card key={c.criterionId} size="small">
            <div className="flex items-start justify-between gap-3">
              <p className="font-semibold text-neutral-900">
                {c.code && <span className="mr-2 font-mono text-brand-600">{c.code}</span>}
                {c.name}
              </p>
              {c.score !== null && c.maxScore === 9 ? (
                <BandBadge band={c.score} />
              ) : (
                <span className="shrink-0 font-semibold text-neutral-900 tabular-nums">
                  {c.score === null ? '—' : formatScore(c.score)} / {formatScore(c.maxScore)}
                </span>
              )}
            </div>
            {c.comment && <p className="mt-1 text-small whitespace-pre-line text-neutral-700">{c.comment}</p>}
            {marks.length > 0 && (
              <ul className="m-0 mt-2 flex list-none flex-col gap-1.5 p-0 text-small">
                {marks.map((item, i) => {
                  // Mục "Giới hạn điểm" được tick là điểm yếu bị nêu (màu cảnh báo), không phải "đạt"
                  const good = item.kind === 'CHECK' && item.checked;
                  const flagged = item.kind === 'LIMIT' && item.checked;
                  return (
                    <li key={i} className="flex items-start gap-2">
                      <span
                        className={clsx(
                          'mt-0.5 inline-flex size-4 shrink-0 items-center justify-center rounded-full border',
                          good && 'border-success bg-success text-neutral-0',
                          flagged && 'border-warning bg-warning text-neutral-0',
                          !good && !flagged && 'border-neutral-300'
                        )}
                      >
                        {(good || flagged) && <Check size={11} />}
                      </span>
                      <span className={clsx(item.kind === 'CHECK' && !item.checked && 'text-neutral-500')}>
                        {item.content}
                      </span>
                    </li>
                  );
                })}
              </ul>
            )}
            {c.limitedTo !== null && c.limitReason && (
              <Alert
                type="warning"
                showIcon
                className="mt-2"
                title={`${c.code ?? c.name} bị giới hạn ở ${formatScore(c.limitedTo)} vì: ${c.limitReason}`}
              />
            )}
          </Card>
        );
      })}

      {result.generalComment && (
        <Card size="small" title="Nhận xét chung">
          <p className="text-small whitespace-pre-line text-neutral-700">{result.generalComment}</p>
          {result.graderName && <p className="mt-2 text-caption text-neutral-500">— {result.graderName}</p>}
        </Card>
      )}
    </div>
  );
}
