import { Tooltip } from 'antd';

import { formatBand } from '@/lib/theme/status';

// ─── Thanh ngang: điểm trung bình theo tiêu chí (thang 0 – 9) ─────────────────
// Một chuỗi nên một màu (brand); giá trị ghi bên phải bằng chữ thường, không tô màu chuỗi.
// Trung bình hiện 2 chữ số như server trả (không làm tròn kiểu IELTS).

export interface CriteriaBarItem {
  key: string;
  label: string;
  /** Dòng phụ dưới nhãn, vd mã tiêu chí. */
  hint?: string;
  average: number | null;
  count: number;
}

const MAX_SCORE = 9;

export function CriteriaBars({ items }: { items: CriteriaBarItem[] }) {
  return (
    <ul className="m-0 flex list-none flex-col gap-3 p-0">
      {items.map((item) => (
        <li key={item.key}>
          <div className="mb-1 flex items-baseline justify-between gap-3">
            <span className="min-w-0 truncate text-small text-neutral-700">
              {item.label}
              {item.hint && <span className="ml-1.5 text-caption text-neutral-400">{item.hint}</span>}
            </span>
            <span className="shrink-0 text-small font-semibold text-neutral-900 tabular-nums">
              {item.average === null ? '—' : item.average.toFixed(2)}
            </span>
          </div>
          <Tooltip
            title={
              item.average === null
                ? 'Chưa có điểm'
                : `Trung bình ${formatBand(item.average)} / ${MAX_SCORE} · ${item.count} lượt chấm`
            }
          >
            <div className="h-2 w-full rounded-full bg-neutral-50">
              <div
                className="h-full rounded-full bg-brand-600"
                style={{ width: `${((item.average ?? 0) / MAX_SCORE) * 100}%` }}
              />
            </div>
          </Tooltip>
        </li>
      ))}
    </ul>
  );
}
