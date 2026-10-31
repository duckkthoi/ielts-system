import { AlertTriangle, Timer, TimerOff, Wifi } from 'lucide-react';

import { StatCard } from '@/components/StatCard';
import type { monitorSummary } from '@/lib/utils/examMonitor';

// ─── 4 thẻ số liệu của màn giám sát ───────────────────────────────────────────
// Bài không có phần ở chế độ thi thì các thẻ về thi hiện "—".

export function MonitorStats({
  online,
  hasExam,
  summary
}: {
  online: number;
  hasExam: boolean;
  summary: ReturnType<typeof monitorSummary>;
}) {
  const exam = (n: number) => (hasExam ? n : '—');
  return (
    <div className="mb-4 grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
      <StatCard label="Đang trực tuyến" value={online} hint="Sinh viên có kết nối" icon={<Wifi size={20} />} />
      <StatCard
        label="Đang thi"
        value={exam(summary.inProgress)}
        hint={hasExam ? `${summary.finished} đã nộp` : 'Bài không có phần ở chế độ thi'}
        icon={<Timer size={20} />}
      />
      <StatCard
        label="Quá thời gian"
        value={exam(summary.overtime)}
        hint="Hết giờ mà chưa nộp"
        icon={<TimerOff size={20} />}
      />
      <StatCard
        label="Có cảnh báo"
        value={exam(summary.flagged)}
        hint="Đã rời trang hoặc dán văn bản"
        icon={<AlertTriangle size={20} />}
      />
    </div>
  );
}
