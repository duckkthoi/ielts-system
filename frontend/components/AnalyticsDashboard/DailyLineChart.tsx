'use client';

import { useState } from 'react';

import { BRAND, NEUTRAL } from '@/lib/theme/tokens';
import { formatDate } from '@/lib/utils/date';

// ─── Đường: số lần nộp theo ngày trong kỳ (một chuỗi, không cần chú giải) ─────
// Nét 2px, điểm 8px khi rê chuột; đường dọc + nhãn ở ngày đang rê. Lưới mờ một mốc (giá trị lớn nhất).
// Kỳ dài hơn 60 ngày gộp theo tuần để đường không quá dày (ghi rõ ở tiêu đề).

const W = 720;
const H = 200;
const PAD = { left: 32, right: 12, top: 16, bottom: 24 };

export function DailyLineChart({ days }: { days: { date: string; count: number }[] }) {
  const [hover, setHover] = useState<number | null>(null);
  const points = days.length > 60 ? byWeek(days) : days;
  const max = Math.max(1, ...points.map((d) => d.count));
  const innerW = W - PAD.left - PAD.right;
  const innerH = H - PAD.top - PAD.bottom;
  const x = (i: number) => PAD.left + (points.length <= 1 ? innerW / 2 : (i / (points.length - 1)) * innerW);
  const y = (v: number) => PAD.top + innerH - (v / max) * innerH;
  const path = points.map((d, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)},${y(d.count).toFixed(1)}`).join(' ');
  const labelEvery = Math.max(1, Math.ceil(points.length / 7));
  const active = hover === null ? null : points[hover];

  const onMove = (e: React.MouseEvent<SVGRectElement>) => {
    const box = e.currentTarget.getBoundingClientRect();
    const ratio = (e.clientX - box.left) / box.width;
    setHover(Math.max(0, Math.min(points.length - 1, Math.round(ratio * (points.length - 1)))));
  };

  return (
    <figure className="m-0">
      <svg viewBox={`0 0 ${W} ${H}`} className="h-auto w-full" role="img" aria-label="Số lần nộp theo ngày">
        <line x1={PAD.left} x2={W - PAD.right} y1={y(max)} y2={y(max)} stroke={NEUTRAL[100]} strokeDasharray="4 4" />
        <line x1={PAD.left} x2={W - PAD.right} y1={y(0)} y2={y(0)} stroke={NEUTRAL[300]} />
        <text x={PAD.left - 6} y={y(max) + 4} textAnchor="end" fontSize={11} fill={NEUTRAL[500]}>
          {max}
        </text>
        <text x={PAD.left - 6} y={y(0) + 4} textAnchor="end" fontSize={11} fill={NEUTRAL[500]}>
          0
        </text>
        {points.map((d, i) =>
          i % labelEvery === 0 || i === points.length - 1 ? (
            <text key={d.date} x={x(i)} y={H - 6} textAnchor="middle" fontSize={11} fill={NEUTRAL[500]}>
              {formatDate(d.date).slice(0, 5)}
            </text>
          ) : null
        )}
        <path d={path} fill="none" stroke={BRAND[600]} strokeWidth={2} strokeLinejoin="round" />
        {active && hover !== null && (
          <g>
            <line x1={x(hover)} x2={x(hover)} y1={PAD.top} y2={y(0)} stroke={NEUTRAL[300]} />
            <circle cx={x(hover)} cy={y(active.count)} r={4} fill={BRAND[600]} stroke={NEUTRAL[0]} strokeWidth={2} />
          </g>
        )}
        <rect
          x={PAD.left}
          y={PAD.top}
          width={innerW}
          height={innerH}
          fill="transparent"
          onMouseMove={onMove}
          onMouseLeave={() => setHover(null)}
        />
      </svg>
      <p className="m-0 h-5 text-center text-small text-neutral-700 tabular-nums" aria-live="polite">
        {active
          ? `${days.length > 60 ? 'Tuần từ ' : ''}${formatDate(active.date)}: ${active.count} lần nộp`
          : days.length > 60
            ? 'Gộp theo tuần'
            : ''}
      </p>
      <figcaption className="sr-only">{points.map((d) => `${formatDate(d.date)}: ${d.count}`).join('; ')}</figcaption>
    </figure>
  );
}

function byWeek(days: { date: string; count: number }[]) {
  const out: { date: string; count: number }[] = [];
  days.forEach((d, i) => {
    if (i % 7 === 0) out.push({ date: d.date, count: 0 });
    out[out.length - 1].count += d.count;
  });
  return out;
}
