// ─── Tiêu đề trang: h1 + mô tả + vùng nút bên phải (docs/07 mục 7.2) ──────────

export function PageHeader({
  title,
  description,
  extra
}: {
  title: string;
  description?: React.ReactNode;
  extra?: React.ReactNode;
}) {
  return (
    <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
      <div className="min-w-0">
        <h1 className="text-h1 font-semibold text-neutral-900">{title}</h1>
        {description && <p className="mt-1 text-neutral-500">{description}</p>}
      </div>
      {extra && <div className="flex flex-wrap items-center gap-2">{extra}</div>}
    </div>
  );
}
