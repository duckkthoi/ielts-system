import clsx from 'clsx';

// ─── Logo chữ "IW" ────────────────────────────────────────────────────────────
// `onBrand`: đặt trên nền màu brand (panel trái trang đăng nhập) → nền trong suốt, viền trắng.

export function BrandMark({ size = 'md', onBrand = false }: { size?: 'md' | 'lg'; onBrand?: boolean }) {
  return (
    <span
      aria-hidden
      className={clsx(
        'grid shrink-0 place-items-center font-bold select-none',
        size === 'lg' ? 'size-13 rounded-[14px] text-lg' : 'size-9 rounded-[10px] text-body',
        onBrand
          ? 'border border-neutral-0/25 bg-neutral-0/10 text-neutral-0'
          : 'bg-brand-600 text-neutral-0 shadow-md shadow-brand-600/20'
      )}
    >
      IW
    </span>
  );
}
