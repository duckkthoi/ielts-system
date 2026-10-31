import { Card } from 'antd';
import { ArrowDownRight, ArrowUpRight, Minus } from 'lucide-react';

import type { AnalyticsOverview, Ratio } from '@/lib/types/analytics';
import { delta, formatRatio } from '@/lib/utils/analytics';

// ─── Hàng KPI có so sánh kỳ trước (cùng độ dài, liền trước) ───────────────────
// Mũi tên + chữ "so với kỳ trước" (không chỉ dùng màu); tỉ lệ luôn ghi tử / mẫu.

function Trend({ value, unit = '', goodWhenUp = true }: { value: number | null; unit?: string; goodWhenUp?: boolean }) {
  if (value === null) return <span className="text-neutral-500">Chưa đủ dữ liệu kỳ trước</span>;
  if (value === 0) {
    return (
      <span className="inline-flex items-center gap-1 text-neutral-500">
        <Minus size={12} /> Không đổi so với kỳ trước
      </span>
    );
  }
  const up = value > 0;
  const good = up === goodWhenUp;
  return (
    <span className={`inline-flex items-center gap-1 ${good ? 'text-success' : 'text-error'}`}>
      {up ? <ArrowUpRight size={12} /> : <ArrowDownRight size={12} />}
      {up ? '+' : ''}
      {value}
      {unit} so với kỳ trước
    </span>
  );
}

function Kpi({ label, value, hint, trend }: { label: string; value: string; hint?: string; trend: React.ReactNode }) {
  return (
    <Card styles={{ body: { padding: 20 } }}>
      <p className="text-caption font-medium text-neutral-500">{label}</p>
      <p className="mt-1 text-display font-semibold text-neutral-900 tabular-nums">{value}</p>
      {hint && <p className="mt-0.5 text-small text-neutral-500">{hint}</p>}
      <p className="mt-1 text-caption">{trend}</p>
    </Card>
  );
}

const pct = (r: Ratio) => (r.rate === null ? null : Math.round(r.rate * 1000) / 10);

export function KpiRow({ o }: { o: AnalyticsOverview }) {
  const k = o.kpis;
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Kpi
        label="Lần nộp trong kỳ"
        value={String(k.submissions.current)}
        hint="Bài nhóm Paper tính một lần"
        trend={<Trend value={delta(k.submissions.current, k.submissions.previous)} />}
      />
      <Kpi
        label="Nộp đúng hạn"
        value={k.onTime.current.rate === null ? '—' : `${Math.round(k.onTime.current.rate * 100)}%`}
        hint={formatRatio(k.onTime.current)}
        trend={<Trend value={delta(pct(k.onTime.current), pct(k.onTime.previous))} unit=" điểm %" />}
      />
      <Kpi
        label="Band TB bài trả trong kỳ"
        value={k.avgBand.current === null ? '—' : k.avgBand.current.toFixed(2)}
        hint="Bài luận IELTS, band từng phần"
        trend={<Trend value={delta(k.avgBand.current, k.avgBand.previous)} />}
      />
      <Kpi
        label="Hoàn thành"
        value={o.units.completion.rate === null ? '—' : `${Math.round(o.units.completion.rate * 100)}%`}
        hint={`${formatRatio(o.units.completion)} lượt (sinh viên × phần)`}
        trend={<span className="text-neutral-500">Tính đến hiện tại · {o.units.overdue} lượt quá hạn</span>}
      />
    </div>
  );
}
