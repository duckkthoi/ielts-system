import { Checkbox, Input, Tag } from 'antd';
import clsx from 'clsx';
import { Lock } from 'lucide-react';

import { BandBadge } from '@/components/BandBadge';
import { DescriptorPanel } from '@/components/DescriptorPanel';
import type { TaskType } from '@/lib/types/assignment';
import type { GradingCriterion } from '@/lib/types/grading';

import { ScorePicker } from './ScorePicker';

// ─── Một tiêu chí trên màn chấm (docs/07 mục 7.2 CriterionCard, Prompt 6b ý 2) ─
// Checklist "Đạt" (CHECK) và "Giới hạn điểm" (LIMIT, tick thì nền warning + khoá các mức cao hơn), ScorePicker,
// ô nhận xét. Viền trái brand-600 khi đã chấm. Tiêu chí IELTS có nút ⓘ mở bảng mô tả band (DescriptorPanel).

const fmt = (n: number) => (Number.isInteger(n) ? String(n) : n.toFixed(1));

export function CriterionCard({
  criterion,
  taskType,
  score,
  comment,
  checked,
  cap,
  disabled,
  onScore,
  onComment,
  onToggle
}: {
  criterion: GradingCriterion;
  taskType: TaskType;
  score: number | null;
  comment: string;
  checked: Record<number, boolean>;
  cap: { max: number; reason: string | null };
  disabled: boolean;
  onScore: (score: number | null) => void;
  onComment: (comment: string) => void;
  onToggle: (itemId: number, value: boolean) => void;
}) {
  return (
    <section
      className={clsx(
        'flex flex-col gap-3 rounded-lg border border-neutral-100 bg-neutral-0 p-4',
        score !== null && 'border-l-4 border-l-brand-600'
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="font-semibold text-neutral-900">
            {criterion.code && <span className="mr-2 font-mono text-brand-600">{criterion.code}</span>}
            {criterion.name}
            {criterion.code && (
              <DescriptorPanel code={criterion.code} name={criterion.name} taskType={taskType} score={score} />
            )}
          </h3>
          {criterion.description && <p className="text-small text-neutral-500">{criterion.description}</p>}
        </div>
        <BandBadge band={score} />
      </div>

      {criterion.checklist.length > 0 && (
        <ul className="m-0 flex list-none flex-col gap-1 p-0">
          {criterion.checklist.map((item) => {
            const on = Boolean(checked[item.id]);
            const limit = item.kind === 'LIMIT';
            return (
              <li
                key={item.id}
                className={clsx(
                  'flex items-start justify-between gap-2 rounded-md px-2 py-1',
                  limit && on && 'bg-warning-bg'
                )}
              >
                <Checkbox checked={on} disabled={disabled} onChange={(e) => onToggle(item.id, e.target.checked)}>
                  <span className={clsx(on && !limit && 'text-neutral-500')}>{item.content}</span>
                </Checkbox>
                {limit && item.maxBand !== null && (
                  <Tag color="warning" className="m-0 inline-flex shrink-0 items-center gap-1">
                    <Lock size={11} /> Tối đa {fmt(item.maxBand)}
                  </Tag>
                )}
              </li>
            );
          })}
        </ul>
      )}

      <ScorePicker
        label={criterion.name}
        value={score}
        maxScore={criterion.maxScore}
        step={criterion.step}
        cap={cap.max}
        capReason={cap.reason}
        disabled={disabled}
        onChange={onScore}
      />
      {cap.reason && (
        <p className="text-small text-warning">
          Bị giới hạn tối đa {fmt(cap.max)} vì: {cap.reason}
        </p>
      )}

      <Input.TextArea
        value={comment}
        onChange={(e) => onComment(e.target.value)}
        disabled={disabled}
        maxLength={2000}
        autoSize={{ minRows: 2, maxRows: 6 }}
        placeholder="Nhận xét cho tiêu chí này…"
      />
    </section>
  );
}
