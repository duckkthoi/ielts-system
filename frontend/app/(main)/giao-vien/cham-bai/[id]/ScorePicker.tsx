import { Tooltip } from 'antd';
import clsx from 'clsx';
import { Lock } from 'lucide-react';

import { scoreOptions } from '@/lib/utils/grading';

// ─── Chọn điểm một tiêu chí (docs/07 mục 7.2, Prompt 6b ý 1) ──────────────────
// Dải nút 0 → maxScore theo bước của tiêu chí (BR-11). Mức lớn hơn giới hạn (BR-17, BR-18) bị mờ + ổ khoá,
// tooltip ghi lý do. Bấm lại mức đang chọn để bỏ chọn (chưa chấm). Phím ← → để đổi mức.

const fmt = (n: number) => (Number.isInteger(n) ? String(n) : n.toFixed(1));

export function ScorePicker({
  value,
  maxScore,
  step,
  cap,
  capReason,
  disabled = false,
  onChange,
  label
}: {
  value: number | null;
  maxScore: number;
  step: number;
  cap: number;
  capReason: string | null;
  disabled?: boolean;
  onChange: (score: number | null) => void;
  label: string;
}) {
  const options = scoreOptions(maxScore, step);
  const allowed = options.filter((o) => o <= cap);

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (disabled || (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight')) return;
    e.preventDefault();
    const idx = value === null ? -1 : allowed.indexOf(value);
    const next = e.key === 'ArrowRight' ? Math.min(allowed.length - 1, idx + 1) : Math.max(0, idx - 1);
    if (allowed[next] !== undefined) onChange(allowed[next]);
  };

  return (
    <div role="radiogroup" aria-label={`Điểm ${label}`} className="flex flex-wrap gap-1" onKeyDown={onKeyDown}>
      {options.map((o) => {
        const locked = o > cap;
        const selected = value === o;
        const button = (
          <button
            key={o}
            type="button"
            role="radio"
            aria-checked={selected}
            tabIndex={selected || (value === null && o === 0) ? 0 : -1}
            disabled={disabled || locked}
            onClick={() => onChange(selected ? null : o)}
            className={clsx(
              'relative inline-flex h-8 min-w-8 items-center justify-center rounded-md border px-1.5 text-small font-medium tabular-nums transition-colors',
              selected
                ? 'border-brand-600 bg-brand-600 text-neutral-0'
                : 'border-neutral-100 bg-neutral-0 text-neutral-700',
              !selected && !locked && !disabled && 'cursor-pointer hover:border-brand-500 hover:text-brand-600',
              locked && 'cursor-not-allowed border-dashed bg-neutral-25 text-neutral-300',
              disabled && !locked && !selected && 'cursor-default opacity-60'
            )}
          >
            {fmt(o)}
            {locked && <Lock size={9} className="absolute -top-1 -right-1 text-neutral-400" />}
          </button>
        );
        return locked && capReason ? (
          <Tooltip key={o} title={`Tối đa ${fmt(cap)} vì: ${capReason}`}>
            <span>{button}</span>
          </Tooltip>
        ) : (
          button
        );
      })}
    </div>
  );
}
