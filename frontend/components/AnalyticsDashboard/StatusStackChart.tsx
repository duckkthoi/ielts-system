'use client';

import { Tooltip } from 'antd';

import { CHART_STATUS_STACK, NEUTRAL } from '@/lib/theme/tokens';
import type { AnalyticsOverview } from '@/lib/types/analytics';

// ─── Cột ngang chồng: trạng thái (sinh viên × phần) của từng bài ──────────────
// Thứ tự cố định Chưa làm · Đang làm · Cần sửa · Đã nộp · Đã trả bài; khe 2px giữa các đoạn, đầu thanh bo 4px.
// Chú giải luôn hiện; rê chuột vào đoạn → số + tỉ lệ. Quá hạn ghi bằng chữ bên phải (không phải một đoạn màu).

type Row = AnalyticsOverview['byAssignment'][number];

const SEGMENTS: { key: keyof Row; label: string; color: string }[] = [
  { key: 'notStarted', label: 'Chưa làm', color: NEUTRAL[100] },
  { key: 'inProgress', label: 'Đang làm', color: CHART_STATUS_STACK.inProgress },
  { key: 'revision', label: 'Cần sửa', color: CHART_STATUS_STACK.revision },
  { key: 'submitted', label: 'Đã nộp', color: CHART_STATUS_STACK.submitted },
  { key: 'returned', label: 'Đã trả bài', color: CHART_STATUS_STACK.returned }
];

export function StatusStackChart({ rows }: { rows: Row[] }) {
  return (
    <figure className="m-0 flex flex-col gap-3">
      <ul className="m-0 flex list-none flex-wrap gap-x-4 gap-y-1 p-0 text-caption text-neutral-700">
        {SEGMENTS.map((s) => (
          <li key={s.key} className="inline-flex items-center gap-1.5">
            <span className="size-2.5 rounded-sm border border-neutral-300" style={{ background: s.color }} />
            {s.label}
          </li>
        ))}
      </ul>
      <ul className="m-0 flex list-none flex-col gap-2.5 p-0">
        {rows.map((r) => {
          const total = SEGMENTS.reduce((sum, s) => sum + Number(r[s.key]), 0);
          return (
            <li
              key={r.assignmentId}
              className="grid grid-cols-1 gap-1 sm:grid-cols-[200px_minmax(0,1fr)_88px] sm:items-center"
            >
              <span className="truncate text-small text-neutral-900" title={r.title}>
                {r.title}
                {r.kind === 'PAPER' && <span className="text-neutral-500"> · Paper</span>}
              </span>
              <div className="flex h-3.5 gap-0.5 overflow-hidden rounded-sm">
                {total === 0 ? (
                  <span className="flex-1 rounded-sm bg-neutral-50" />
                ) : (
                  SEGMENTS.map((s) => {
                    const n = Number(r[s.key]);
                    if (n === 0) return null;
                    return (
                      <Tooltip key={s.key} title={`${s.label}: ${n}/${total} (${Math.round((n / total) * 100)}%)`}>
                        <span
                          className="h-full first:rounded-l-sm last:rounded-r-sm"
                          style={{ flexGrow: n, flexBasis: 0, background: s.color, minWidth: 3 }}
                        />
                      </Tooltip>
                    );
                  })
                )}
              </div>
              <span className="text-caption text-neutral-500 tabular-nums sm:text-right">
                {total} lượt{r.overdue > 0 && <span className="text-error"> · {r.overdue} quá hạn</span>}
              </span>
            </li>
          );
        })}
      </ul>
      <figcaption className="sr-only">
        {rows.map((r) => `${r.title}: ${SEGMENTS.map((s) => `${s.label} ${Number(r[s.key])}`).join(', ')}`).join('; ')}
      </figcaption>
    </figure>
  );
}
