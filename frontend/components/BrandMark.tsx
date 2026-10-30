import clsx from 'clsx';

// ─── Logo AcaWrite: ngòi bút hướng lên (đồng thời là chữ "A") trên một dòng kẻ ─
// `onBrand`: đặt trên nền màu brand (panel trái trang đăng nhập) → nền trong suốt, viền trắng.
// Hình giữ đồng bộ với favicon `app/icon.svg` (docs/07 mục 1.1).

export function BrandMark({ size = 'md', onBrand = false }: { size?: 'md' | 'lg'; onBrand?: boolean }) {
  return (
    <span
      aria-hidden
      className={clsx(
        'grid shrink-0 place-items-center select-none',
        size === 'lg' ? 'size-13 rounded-[14px]' : 'size-9 rounded-[10px]',
        onBrand
          ? 'border border-neutral-0/25 bg-neutral-0/10 text-neutral-0'
          : 'bg-brand-600 text-neutral-0 shadow-md shadow-brand-600/20'
      )}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
        className={size === 'lg' ? 'size-7.5' : 'size-5.5'}
      >
        <path d="M12 2.5 18 11l-1.6 6.5H7.6L6 11Z" />
        <path d="M12 2.5V9" />
        <circle cx="12" cy="11.2" r="1.6" />
        <path d="M4 21.5h16" />
      </svg>
    </span>
  );
}
