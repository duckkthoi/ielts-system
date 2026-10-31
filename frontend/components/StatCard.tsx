import { Card } from 'antd';

// ─── Thẻ số liệu dashboard (docs/07 mục 7.2): nhãn caption, số display, dòng phụ ─
// Điện thoại (< sm) ẩn ô icon để xếp được 2 thẻ một hàng.

export function StatCard({
  label,
  value,
  hint,
  icon
}: {
  label: string;
  value: React.ReactNode;
  /** Dòng phụ dưới con số, vd "2 tài khoản bị khoá". */
  hint?: React.ReactNode;
  icon?: React.ReactNode;
}) {
  return (
    <Card styles={{ body: { padding: 20 } }}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-caption font-medium text-neutral-500">{label}</p>
          <p className="mt-1 text-display font-semibold text-neutral-900 tabular-nums">{value}</p>
          {hint && <p className="mt-1 text-small text-neutral-500">{hint}</p>}
        </div>
        {icon && (
          <span className="hidden size-10 shrink-0 place-items-center rounded-md bg-brand-50 text-brand-600 sm:grid">
            {icon}
          </span>
        )}
      </div>
    </Card>
  );
}
