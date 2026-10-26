'use client';

import { Empty } from 'antd';
import clsx from 'clsx';

import type { AnnotationView } from '@/components/EssayAnnotator';
import { CATEGORY_DOT, KIND_LABEL } from '@/components/EssayAnnotator/const';
import { CATEGORY_LABEL, effectiveEdit, errorStats } from '@/lib/utils/annotations';
import type { ErrorCodeDto } from '@/lib/types/grading';

// ─── Panel phải, tab "Đánh dấu": thống kê số lỗi + danh sách (bấm → cuộn tới đoạn trên bài) ─

const KIND_DOT: Record<AnnotationView['kind'], string> = {
  ERROR: 'bg-error',
  HIGHLIGHT: 'bg-highlight-comment-line',
  GOOD: 'bg-success',
  COMMENT: 'bg-neutral-400'
};

export function AnnotationList({
  annotations,
  errorCodes,
  selectedKey,
  onPick
}: {
  annotations: AnnotationView[];
  errorCodes: ErrorCodeDto[];
  selectedKey: string | null;
  onPick: (key: string) => void;
}) {
  const stats = errorStats(
    annotations,
    (code) => errorCodes.find((c) => c.code === code)?.criterion ?? null,
    (a) => a.criterionLabel
  );
  const sorted = [...annotations].sort((a, b) => a.startOffset - b.startOffset);

  return (
    <div className="flex flex-col gap-3 p-4 sm:p-5">
      <section className="rounded-lg border border-neutral-100 bg-neutral-0 p-4">
        <h3 className="font-semibold text-neutral-900">
          Thống kê lỗi · <span className="tabular-nums">{stats.total}</span> lỗi
        </h3>
        {stats.total === 0 ? (
          <p className="mt-1 text-small text-neutral-500">Chưa có lỗi nào được gắn loại lỗi.</p>
        ) : (
          <div className="mt-2 flex flex-col gap-2">
            <div className="flex flex-wrap gap-1.5">
              {stats.byCategory.map(([cat, n]) => (
                <span
                  key={cat}
                  className="inline-flex items-center gap-1.5 rounded-full border border-neutral-100 px-2 py-0.5 text-small"
                >
                  <span className={clsx('size-2 rounded-full', CATEGORY_DOT[cat])} />
                  {CATEGORY_LABEL[cat]} <strong className="tabular-nums">×{n}</strong>
                </span>
              ))}
            </div>
            <p className="text-small text-neutral-500">
              Theo tiêu chí:{' '}
              {stats.byCriterion.map(([crit, n], i) => (
                <span key={crit}>
                  {i > 0 && ' · '}
                  {crit} <strong className="tabular-nums">{n}</strong>
                </span>
              ))}
            </p>
          </div>
        )}
      </section>

      {sorted.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="Chưa có đánh dấu. Bôi đen chữ trong bài để thêm lỗi, đánh dấu hoặc nhận xét."
        />
      ) : (
        <ul className="m-0 flex list-none flex-col gap-1.5 p-0">
          {sorted.map((a) => {
            const edit = effectiveEdit(a);
            return (
              <li key={a.key}>
                <button
                  type="button"
                  onClick={() => onPick(a.key)}
                  className={clsx(
                    'flex w-full cursor-pointer flex-col gap-1 rounded-md border px-3 py-2 text-left text-small transition-colors',
                    selectedKey === a.key
                      ? 'border-brand-500 bg-brand-50'
                      : 'border-neutral-100 bg-neutral-0 hover:border-brand-200'
                  )}
                >
                  <span className="flex items-center gap-1.5 text-caption text-neutral-500">
                    <span
                      className={clsx('size-2 rounded-full', a.category ? CATEGORY_DOT[a.category] : KIND_DOT[a.kind])}
                    />
                    {KIND_LABEL[a.kind]}
                    {a.errorCode && (
                      <>
                        {' · '}
                        <span className="font-mono font-semibold text-neutral-700">{a.errorCode}</span>
                      </>
                    )}
                    {a.criterionLabel && ` · ${a.criterionLabel}`}
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
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
