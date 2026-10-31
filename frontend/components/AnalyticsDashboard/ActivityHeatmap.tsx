'use client';

import { Tooltip } from 'antd';

import { CHART_HEAT, NEUTRAL } from '@/lib/theme/tokens';
import { heatGrid, heatLevel } from '@/lib/utils/analytics';

// ─── Bản đồ nhiệt: hoạt động (lần nộp + cập nhật tài liệu Paper) theo thứ × giờ ─
// Một sắc độ, nhạt → đậm theo 4 mức của giá trị lớn nhất; ô trống = không có hoạt động. Rê chuột → số liệu.
// Trên điện thoại cuộn ngang trong khung, không làm tràn trang.

const DAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

export function ActivityHeatmap({
  cells,
  zone
}: {
  cells: { dow: number; hour: number; count: number }[];
  zone: string;
}) {
  const grid = heatGrid(cells);
  const max = Math.max(0, ...grid.flat());

  return (
    <figure className="m-0">
      <div className="overflow-x-auto">
        <div className="inline-grid min-w-max grid-cols-[28px_repeat(24,18px)] gap-0.5 text-caption text-neutral-500">
          <span />
          {Array.from({ length: 24 }, (_, h) => (
            <span key={h} className="text-center tabular-nums">
              {h % 3 === 0 ? h : ''}
            </span>
          ))}
          {grid.map((row, d) => (
            <div key={DAYS[d]} className="contents">
              <span className="leading-[18px]">{DAYS[d]}</span>
              {row.map((v, h) => {
                const level = heatLevel(v, max);
                return (
                  <Tooltip key={h} title={`${DAYS[d]}, ${h}:00–${h}:59: ${v} hoạt động`}>
                    <span
                      className="size-4.5 rounded-sm"
                      style={{ background: level === 0 ? NEUTRAL[50] : CHART_HEAT[level - 1] }}
                    />
                  </Tooltip>
                );
              })}
            </div>
          ))}
        </div>
      </div>
      <div className="mt-2 flex flex-wrap items-center gap-1.5 text-caption text-neutral-500">
        Ít
        {[NEUTRAL[50], ...CHART_HEAT].map((c) => (
          <span key={c} className="size-3 rounded-sm" style={{ background: c }} />
        ))}
        Nhiều · giờ theo {zone}
      </div>
      <figcaption className="sr-only">{grid.map((row, d) => `${DAYS[d]}: ${row.join(', ')}`).join('; ')}</figcaption>
    </figure>
  );
}
