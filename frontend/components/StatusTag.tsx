import type { ColorSet } from '@/lib/theme/tokens';

// ─── Tag trạng thái hình viên thuốc, có chấm màu (docs/07 mục 7.2) ────────────
// Màu + nhãn luôn lấy từ lib/theme/status.ts (vd ASSIGNMENT_STATUS_META[status]).

export function StatusTag({ meta, size = 'md' }: { meta: ColorSet & { label: string }; size?: 'sm' | 'md' }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border font-medium whitespace-nowrap ${
        size === 'sm' ? 'px-2 py-px text-caption' : 'px-2.5 py-0.5 text-small'
      }`}
      style={{ color: meta.text, background: meta.bg, borderColor: meta.border }}
    >
      <span className="size-1.5 rounded-full" style={{ background: meta.text }} />
      {meta.label}
    </span>
  );
}
