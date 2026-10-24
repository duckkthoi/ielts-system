import clsx from 'clsx';

import { formatBand, getBandLevel } from '@/lib/theme/status';

// ─── Band IELTS trong vòng tròn, màu theo mức (docs/07 mục 2.6, 7.2) ──────────
// null → "—" nền xám. Màu lấy từ BAND_LEVELS, không tự chọn.

const SIZE_CLASS = {
  sm: 'size-6 text-caption',
  md: 'size-8 text-small',
  lg: 'size-14 text-h2'
} as const;

export function BandBadge({ band, size = 'md' }: { band: number | null | undefined; size?: keyof typeof SIZE_CLASS }) {
  const level = band === null || band === undefined ? null : getBandLevel(band);
  return (
    <span
      className={clsx(
        'inline-flex shrink-0 items-center justify-center rounded-full font-semibold tabular-nums',
        SIZE_CLASS[size],
        !level && 'bg-neutral-50 text-neutral-400'
      )}
      style={level ? { color: level.text, background: level.bg } : undefined}
      title={level ? `Band ${formatBand(band)} · ${level.label}` : 'Chưa có band'}
    >
      {formatBand(band)}
    </span>
  );
}
