import clsx from 'clsx';

import type { LevelDto } from '@/lib/types/assignment';
import { formatScore } from '@/lib/utils/grading';

// ─── Chọn mức đạt của một tiêu chí bài PAPER (rubric phân tích, BR-31, docs/12 mục 5.4) ─
// Mỗi ô = một mức: tên, khoảng điểm, mô tả. Bấm ô → điểm nhảy vào khoảng của mức (useGradeForm.setLevel), sau đó
// chỉnh điểm trong khoảng bằng ScorePicker. Bấm lại mức đang chọn để bỏ chọn. Mức có điểm thấp nhất > giới hạn hiện
// tại (checklist "Giới hạn điểm") bị khoá.

export function LevelPicker({
  label,
  levels,
  value,
  cap,
  disabled,
  onChange
}: {
  label: string;
  levels: LevelDto[];
  value: number | null;
  cap: number;
  disabled: boolean;
  onChange: (level: LevelDto | null) => void;
}) {
  const sorted = [...levels].sort((a, b) => b.maxScore - a.maxScore);
  return (
    <div role="radiogroup" aria-label={`Mức đạt ${label}`} className="grid grid-cols-1 gap-2 sm:grid-cols-2">
      {sorted.map((l) => {
        const selected = value === l.id;
        const locked = l.minScore > cap;
        return (
          <button
            key={l.id}
            type="button"
            role="radio"
            aria-checked={selected}
            disabled={disabled || locked}
            title={locked ? `Đang bị giới hạn tối đa ${formatScore(cap)}` : undefined}
            onClick={() => onChange(selected ? null : l)}
            className={clsx(
              'flex flex-col gap-1 rounded-md border p-2 text-left transition-colors',
              selected ? 'border-brand-600 bg-brand-50' : 'border-neutral-100 bg-neutral-0',
              !selected && !locked && !disabled && 'cursor-pointer hover:border-brand-500',
              locked && 'cursor-not-allowed border-dashed opacity-50',
              disabled && !locked && !selected && 'cursor-default opacity-60'
            )}
          >
            <span className="flex items-center justify-between gap-2">
              <span className={clsx('font-semibold', selected ? 'text-brand-600' : 'text-neutral-900')}>{l.label}</span>
              <span className="text-caption text-neutral-500 tabular-nums">
                {formatScore(l.minScore)}–{formatScore(l.maxScore)}
              </span>
            </span>
            {l.descriptor && <span className="text-small text-neutral-700">{l.descriptor}</span>}
          </button>
        );
      })}
    </div>
  );
}
