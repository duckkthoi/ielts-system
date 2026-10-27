'use client';

import { useEffect, useMemo, useRef, useState } from 'react';

import { Tooltip } from 'antd';

import { formatBand } from '@/lib/theme/status';
import { CHART_SERIES, NEUTRAL } from '@/lib/theme/tokens';
import { bandDomain, timeScale } from '@/lib/utils/bandTrend';
import type { TrendSeries } from '@/lib/utils/bandTrend';
import { formatDate } from '@/lib/utils/date';

// ─── Biểu đồ đường: band theo thời điểm trả bài ───────────────────────────────
// Vẽ theo bề rộng thật của khung (đo bằng ResizeObserver) để chữ trục không bị co trên điện thoại.
// Đường 2px, điểm 8px có viền trắng; rê chuột vào vùng quanh điểm để xem bài, band, ngày trả.
// ≥ 2 chuỗi thì có chú thích phía trên; màu theo chuỗi (CHART_SERIES), không theo số chuỗi.

const HEIGHT = 220;
const PAD = { top: 12, right: 16, bottom: 28, left: 32 };
/** Khoảng cách điểm đầu / cuối với mép vùng vẽ, để điểm không dính nhãn trục. */
const INSET = 14;
const SERIES_COLOR: Record<TrendSeries['key'], string> = {
  overall: CHART_SERIES[0],
  task1: CHART_SERIES[1],
  task2: CHART_SERIES[2]
};

function useWidth<T extends HTMLElement>() {
  const ref = useRef<T>(null);
  const [width, setWidth] = useState(0);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const observer = new ResizeObserver(([entry]) => setWidth(Math.floor(entry.contentRect.width)));
    observer.observe(el);
    return () => observer.disconnect();
  }, []);
  return { ref, width };
}

export function BandTrendChart({ series }: { series: TrendSeries[] }) {
  const { ref, width } = useWidth<HTMLDivElement>();

  const { domain, toX } = useMemo(() => {
    const points = series.flatMap((s) => s.points);
    return { domain: bandDomain(points.map((p) => p.band)), toX: timeScale(points.map((p) => p.at)) };
  }, [series]);

  const allTimes = series.flatMap((s) => s.points.map((p) => p.at));
  const first = Math.min(...allTimes);
  const last = Math.max(...allTimes);

  const plotW = Math.max(0, width - PAD.left - PAD.right - 2 * INSET);
  const plotH = HEIGHT - PAD.top - PAD.bottom;
  const x = (at: number) => PAD.left + INSET + toX(at) * plotW;
  const y = (band: number) => PAD.top + (1 - (band - domain[0]) / (domain[1] - domain[0])) * plotH;
  const ticks = Array.from({ length: domain[1] - domain[0] + 1 }, (_, i) => domain[0] + i);

  return (
    <figure className="m-0">
      {series.length > 1 && (
        <div className="mb-2 flex flex-wrap gap-x-4 gap-y-1 text-small text-neutral-700">
          {series.map((s) => (
            <span key={s.key} className="inline-flex items-center gap-1.5">
              <span className="inline-block h-0.5 w-4 rounded-full" style={{ background: SERIES_COLOR[s.key] }} />
              {s.label}
            </span>
          ))}
        </div>
      )}
      <div ref={ref} className="w-full" style={{ height: HEIGHT }}>
        {width > 0 && (
          <svg width={width} height={HEIGHT} role="img" aria-label="Band theo thời gian">
            {ticks.map((t) => (
              <g key={t}>
                <line x1={PAD.left} x2={width - PAD.right} y1={y(t)} y2={y(t)} stroke={NEUTRAL[100]} />
                <text x={PAD.left - 8} y={y(t)} dy="0.32em" textAnchor="end" fontSize={11} fill={NEUTRAL[400]}>
                  {t}
                </text>
              </g>
            ))}
            <text x={PAD.left + INSET} y={HEIGHT - 8} fontSize={11} fill={NEUTRAL[500]}>
              {formatDate(first)}
            </text>
            {last !== first && (
              <text x={width - PAD.right - INSET} y={HEIGHT - 8} textAnchor="end" fontSize={11} fill={NEUTRAL[500]}>
                {formatDate(last)}
              </text>
            )}
            {series.map((s) => (
              <polyline
                key={s.key}
                points={s.points.map((p) => `${x(p.at)},${y(p.band)}`).join(' ')}
                fill="none"
                stroke={SERIES_COLOR[s.key]}
                strokeWidth={2}
                strokeLinejoin="round"
                strokeLinecap="round"
              />
            ))}
            {series.map((s) =>
              s.points.map((p) => (
                <Tooltip
                  key={`${s.key}-${p.key}`}
                  title={
                    <>
                      <div className="font-medium">{p.label}</div>
                      <div>
                        {s.label}: band {formatBand(p.band)} · trả ngày {formatDate(p.at)}
                      </div>
                    </>
                  }
                >
                  <g className="cursor-default">
                    <circle cx={x(p.at)} cy={y(p.band)} r={12} fill="transparent" />
                    <circle
                      cx={x(p.at)}
                      cy={y(p.band)}
                      r={4}
                      fill={SERIES_COLOR[s.key]}
                      stroke={NEUTRAL[0]}
                      strokeWidth={2}
                    />
                  </g>
                </Tooltip>
              ))
            )}
          </svg>
        )}
      </div>
      <figcaption className="sr-only">
        {series
          .map((s) => `${s.label}: ${s.points.map((p) => `${p.label} ${formatBand(p.band)}`).join(', ')}`)
          .join('. ')}
      </figcaption>
    </figure>
  );
}
