'use client';

import { Tooltip } from 'antd';

import { formatBand } from '@/lib/theme/status';
import type { BandCount } from '@/lib/types/stats';
import { fillBandBins } from '@/lib/utils/bandDistribution';

// ─── Biểu đồ cột: số bài theo band (một chuỗi, một màu) ───────────────────────
// Cùng kiểu với biểu đồ bài nộp của trang quản trị: cột bo 4px phía trên, rê chuột vào vùng cột
// (cao cả biểu đồ) để xem số liệu; số chỉ ghi trên cột cao nhất. Mọi mức 0.5 ở giữa đều có cột.

const CHART_HEIGHT = 160;

export function BandDistributionChart({ distribution, unit = 'bài' }: { distribution: BandCount[]; unit?: string }) {
  const bins = fillBandBins(distribution);
  const max = Math.max(1, ...bins.map((b) => b.count));
  const peak = bins.findIndex((b) => b.count === max);

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
        <div className="relative min-w-0 flex-1">
          <div className="absolute inset-x-0 top-0 border-t border-dashed border-neutral-100" />
          <div className="flex items-end gap-0.5 border-b border-neutral-300" style={{ height: CHART_HEIGHT }}>
            {bins.map((bin, index) => (
              <Tooltip key={bin.band} title={`Band ${formatBand(bin.band)}: ${bin.count} ${unit}`}>
                <div className="group flex h-full flex-1 cursor-default flex-col items-center justify-end">
                  {index === peak && (
                    <span className="mb-1 text-caption font-medium text-neutral-700 tabular-nums">{bin.count}</span>
                  )}
                  <div
                    className="w-full max-w-10 rounded-t-sm bg-brand-600 transition-colors group-hover:bg-brand-700"
                    style={{ height: `${(bin.count / max) * 100}%`, minHeight: bin.count > 0 ? 2 : 0 }}
                  />
                </div>
              </Tooltip>
            ))}
          </div>
          <div className="mt-1.5 flex gap-0.5 text-center text-caption text-neutral-500">
            {bins.map((bin) => (
              <span key={bin.band} className="flex-1 tabular-nums">
                {formatBand(bin.band)}
              </span>
            ))}
          </div>
        </div>
      </div>
      <figcaption className="sr-only">
        {bins.map((b) => `Band ${formatBand(b.band)}: ${b.count} ${unit}`).join('; ')}
      </figcaption>
    </figure>
  );
}
