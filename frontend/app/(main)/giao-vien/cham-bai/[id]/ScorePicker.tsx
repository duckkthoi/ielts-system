import { Button, InputNumber, Slider, Tooltip } from 'antd';
import clsx from 'clsx';
import { Lock } from 'lucide-react';

import { formatScore as fmt, scoreOptions } from '@/lib/utils/grading';

// ─── Chọn điểm một tiêu chí (docs/07 mục 7.2, Prompt 6b ý 1) ──────────────────
// Dải nút 0 → maxScore theo bước của tiêu chí (BR-11). Mức lớn hơn giới hạn (BR-17, BR-18) bị mờ + ổ khoá,
// tooltip ghi lý do. Bấm lại mức đang chọn để bỏ chọn (chưa chấm). Phím ← → để đổi mức.
// Bài PAPER đã chọn mức đạt: chỉ hiện các điểm trong khoảng [range.min, range.max] của mức (BR-31).
// Quá MAX_BUTTONS mức (bước nhỏ, vd thang 10 bước 0.25) → thanh trượt + ô nhập số theo đúng bước, trần như trên.

const MAX_BUTTONS = 21;

function CompactScoreInput({
  value,
  options,
  allowed,
  cap,
  capReason,
  disabled,
  onChange,
  label
}: {
  value: number | null;
  options: number[];
  allowed: number[];
  cap: number;
  capReason: string | null;
  disabled: boolean;
  onChange: (score: number | null) => void;
  label: string;
}) {
  const min = options[0];
  const max = allowed.length > 0 ? allowed[allowed.length - 1] : min;
  // Giá trị nhập tay làm tròn về mức gần nhất trong danh sách cho phép.
  const snap = (v: number) =>
    allowed.reduce((best, o) => (Math.abs(o - v) < Math.abs(best - v) ? o : best), allowed[0]);
  const step = options.length > 1 ? options[1] - options[0] : 1;
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center gap-3">
        <Slider
          className="m-0! flex-1"
          min={min}
          max={max}
          step={step}
          value={value ?? undefined}
          disabled={disabled || allowed.length === 0}
          tooltip={{ formatter: (v) => (v === undefined ? '' : fmt(v)) }}
          onChange={(v) => onChange(snap(v))}
          aria-label={`Điểm ${label}`}
        />
        <InputNumber
          className="w-24"
          min={min}
          max={max}
          step={step}
          value={value}
          disabled={disabled || allowed.length === 0}
          placeholder="—"
          onChange={(v) => onChange(v === null ? null : snap(v))}
          aria-label={`Nhập điểm ${label}`}
        />
        {value !== null && !disabled && (
          <Button type="link" size="small" className="px-0" onClick={() => onChange(null)}>
            Bỏ chọn
          </Button>
        )}
      </div>
      <p className="text-caption text-neutral-500 tabular-nums">
        Từ {fmt(min)} đến {fmt(options[options.length - 1])}, bước {fmt(step)}
        {cap < options[options.length - 1] && capReason ? ` · tối đa ${fmt(cap)} vì: ${capReason}` : ''}
      </p>
    </div>
  );
}

export function ScorePicker({
  value,
  maxScore,
  step,
  cap,
  capReason,
  range,
  disabled = false,
  onChange,
  label
}: {
  value: number | null;
  maxScore: number;
  step: number;
  cap: number;
  capReason: string | null;
  range?: { min: number; max: number } | null;
  disabled?: boolean;
  onChange: (score: number | null) => void;
  label: string;
}) {
  const options = scoreOptions(maxScore, step).filter(
    (o) => !range || (o >= range.min - 1e-9 && o <= range.max + 1e-9)
  );
  const allowed = options.filter((o) => o <= cap);

  if (options.length > MAX_BUTTONS) {
    return (
      <CompactScoreInput
        value={value}
        options={options}
        allowed={allowed}
        cap={cap}
        capReason={capReason}
        disabled={disabled}
        onChange={onChange}
        label={label}
      />
    );
  }

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
            tabIndex={selected || (value === null && o === options[0]) ? 0 : -1}
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
