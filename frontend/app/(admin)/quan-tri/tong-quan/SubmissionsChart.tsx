'use client';

import { Tooltip } from 'antd';

import { formatDate } from '@/lib/utils/date';

// ─── Biểu đồ cột: số bài nộp mỗi ngày trong 14 ngày (một chuỗi, một màu) ──────
// Cột mảnh bo 4px phía trên, cách nhau 2px; rê chuột vào vùng cột (cao cả biểu đồ) để xem số liệu.
// Chỉ ghi số trên cột cao nhất và cột hôm nay; trục y là một đường nền mờ + mốc giá trị lớn nhất.

const CHART_HEIGHT = 180;

export function SubmissionsChart({ days }: { days: { date: string; count: number }[] }) {
  const max = Math.max(1, ...days.map((d) => d.count));
  const peakIndex = days.findIndex((d) => d.count === max);
  const lastIndex = days.length - 1;

  return (
    <figure className="m-0">
      <div className="flex gap-3">
        <div
          className="flex flex-col justify-between text-right text-caption text-neutral-400 tabular-nums"
          style={{ height: CHART_HEIGHT }}
        >
          <span>{max}</span>
          <span>0</span>
        </div>
        <div className="relative flex-1">
          <div className="absolute inset-x-0 top-0 border-t border-dashed border-neutral-100" />
          <div className="flex items-end gap-0.5 border-b border-neutral-300" style={{ height: CHART_HEIGHT }}>
            {days.map((day, index) => {
              const showValue = day.count > 0 && (index === peakIndex || index === lastIndex);
              return (
                <Tooltip key={day.date} title={`${formatDate(day.date)}: ${day.count} bài nộp`}>
                  <div className="group flex h-full flex-1 cursor-default flex-col items-center justify-end">
                    {showValue && (
                      <span className="mb-1 text-caption font-medium text-neutral-700 tabular-nums">{day.count}</span>
                    )}
                    <div
                      className="w-full max-w-7 rounded-t-sm bg-brand-600 transition-colors group-hover:bg-brand-700"
                      style={{ height: `${(day.count / max) * 100}%`, minHeight: day.count > 0 ? 2 : 0 }}
                    />
                  </div>
                </Tooltip>
              );
            })}
          </div>
          <div className="mt-1.5 flex gap-0.5 text-center text-caption text-neutral-500">
            {days.map((day, index) => (
              <span key={day.date} className="flex-1 tabular-nums">
                {index % 2 === lastIndex % 2 ? formatDate(day.date).slice(0, 5) : ''}
              </span>
            ))}
          </div>
        </div>
      </div>
      <figcaption className="sr-only">{days.map((d) => `${formatDate(d.date)}: ${d.count}`).join('; ')}</figcaption>
    </figure>
  );
}
