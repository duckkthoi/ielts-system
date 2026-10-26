import clsx from 'clsx';

// ─── Chấm trạng thái online 8px ở góc dưới phải avatar (docs/07 mục 7.2, bảng 2.8) ─
// Có `title` để không chỉ dựa vào màu.

export function PresenceDot({ online, children }: { online: boolean; children: React.ReactNode }) {
  return (
    <span className="relative inline-flex shrink-0">
      {children}
      <span
        className={clsx(
          'absolute right-0 bottom-0 size-2.5 rounded-full ring-2 ring-neutral-0',
          online ? 'bg-presence-online' : 'bg-presence-offline'
        )}
        title={online ? 'Đang trực tuyến' : 'Ngoại tuyến'}
        aria-label={online ? 'Đang trực tuyến' : 'Ngoại tuyến'}
      />
    </span>
  );
}
