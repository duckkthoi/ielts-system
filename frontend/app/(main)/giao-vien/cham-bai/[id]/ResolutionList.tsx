'use client';

import { Button, Empty, Segmented } from 'antd';
import clsx from 'clsx';

import type { AnnotationView } from '@/components/EssayAnnotator';
import { CATEGORY_DOT } from '@/components/EssayAnnotator/const';
import type { ErrorCodeDto, Resolution } from '@/lib/types/grading';
import { CATEGORY_LABEL, effectiveEdit } from '@/lib/utils/annotations';
import { fixSummary } from '@/lib/utils/textDiff';

import { RESOLUTION_META } from './const';

// ─── So sánh: tóm tắt "Đã sửa X/Y" + từng lỗi của lần cũ (tự đoán, giáo viên chỉnh tay được) ─

export interface ResolvedItem {
  id: number;
  view: AnnotationView;
  guess: Resolution;
  /** Giáo viên đã chỉnh tay (lưu ở server); null = theo tự đoán. */
  manual: Resolution | null;
}

const OPTIONS = (['FIXED', 'CHANGED', 'NOT_FIXED'] as const).map((r) => ({
  value: r,
  label: RESOLUTION_META[r].label
}));

export function ResolutionList({
  items,
  errorCodes,
  oldNo,
  selectedId,
  savingId,
  onPick,
  onChange
}: {
  items: ResolvedItem[];
  errorCodes: ErrorCodeDto[];
  oldNo: number;
  selectedId: number | null;
  savingId: number | null;
  onPick: (id: number) => void;
  onChange: (id: number, resolution: Resolution | null) => void;
}) {
  const summary = fixSummary(
    items.map((it) => ({
      errorCode: it.view.errorCode,
      resolution: it.manual ?? it.guess,
      criterionLabel: it.view.criterionLabel
    })),
    (code) => errorCodes.find((c) => c.code === code)?.criterion ?? null
  );

  if (items.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={`Lần ${oldNo} không có lỗi nào được đánh dấu.`} />;
  }

  return (
    <div className="flex flex-col gap-3">
      <section className="rounded-lg border border-neutral-100 bg-neutral-0 p-4">
        <h3 className="font-semibold text-neutral-900">
          Đã sửa{' '}
          <span className="tabular-nums">
            {summary.fixed}/{summary.total}
          </span>{' '}
          lỗi của lần {oldNo}
        </h3>
        <p className="mt-0.5 text-small text-neutral-500">
          Đổi khác gợi ý {summary.changed} · Chưa sửa {summary.notFixed}. Tự đoán theo đoạn trích; bấm để chỉnh nếu đoán
          sai.
        </p>
        {summary.total > 0 && (
          <div className="mt-2 flex flex-col gap-2">
            <div className="flex flex-wrap gap-1.5">
              {summary.byCategory.map(([cat, c]) => (
                <span
                  key={cat}
                  className="inline-flex items-center gap-1.5 rounded-full border border-neutral-100 px-2 py-0.5 text-small"
                >
                  <span className={clsx('size-2 rounded-full', CATEGORY_DOT[cat])} />
                  {CATEGORY_LABEL[cat]}{' '}
                  <strong className="tabular-nums">
                    {c.fixed}/{c.total}
                  </strong>
                </span>
              ))}
            </div>
            <p className="text-small text-neutral-500">
              Theo tiêu chí:{' '}
              {summary.byCriterion.map(([crit, c], i) => (
                <span key={crit}>
                  {i > 0 && ' · '}
                  {crit}{' '}
                  <strong className="tabular-nums">
                    {c.fixed}/{c.total}
                  </strong>
                </span>
              ))}
            </p>
          </div>
        )}
      </section>

      <ul className="m-0 flex list-none flex-col gap-1.5 p-0">
        {items.map(({ id, view: a, guess, manual }) => {
          const edit = effectiveEdit(a);
          const value = manual ?? guess;
          return (
            <li
              key={id}
              className={clsx(
                'flex flex-col gap-1.5 rounded-md border px-3 py-2 text-small',
                selectedId === id ? 'border-brand-500 bg-brand-50' : 'border-neutral-100 bg-neutral-0'
              )}
            >
              <button
                type="button"
                onClick={() => onPick(id)}
                className="flex cursor-pointer flex-col gap-1 border-0 bg-transparent p-0 text-left"
              >
                <span className="flex items-center gap-1.5 text-caption text-neutral-500">
                  <span className={clsx('size-2 rounded-full', a.category ? CATEGORY_DOT[a.category] : 'bg-error')} />
                  {a.errorCode ? (
                    <span className="font-mono font-semibold text-neutral-700">{a.errorCode}</span>
                  ) : (
                    'Lỗi'
                  )}
                  {a.criterionLabel && ` · ${a.criterionLabel}`}
                  <span className={clsx('ml-auto font-semibold', RESOLUTION_META[value].text)}>
                    {RESOLUTION_META[value].label}
                    {!manual && <span className="font-normal text-neutral-400"> (tự đoán)</span>}
                  </span>
                </span>
                <span className="truncate font-serif text-neutral-900">
                  {edit === 'REPLACE' || edit === 'DELETE' ? (
                    <span className="text-annot-edit line-through">{a.quote}</span>
                  ) : (
                    `“${a.quote}”`
                  )}
                  {(edit === 'REPLACE' || edit === 'INSERT') && (
                    <span className="ml-1 text-annot-edit underline">{a.suggestion}</span>
                  )}
                </span>
                {a.comment && <span className="line-clamp-2 text-neutral-500">{a.comment}</span>}
              </button>
              <span className="flex flex-wrap items-center gap-2">
                <Segmented
                  size="small"
                  value={value}
                  disabled={savingId === id}
                  options={OPTIONS}
                  onChange={(v) => onChange(id, v as Resolution)}
                />
                {manual && (
                  <Button size="small" type="link" disabled={savingId === id} onClick={() => onChange(id, null)}>
                    Về tự đoán
                  </Button>
                )}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
