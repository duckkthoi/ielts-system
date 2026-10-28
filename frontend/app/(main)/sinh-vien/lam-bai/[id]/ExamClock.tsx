'use client';

import { useEffect, useState } from 'react';

import clsx from 'clsx';
import { Timer, TimerOff } from 'lucide-react';

import { formatExamClock } from '@/lib/utils/exam';
import { serverNow } from '@/lib/utils/serverClock';

// ─── Đồng hồ chế độ thi (BR-23, BR-24) ────────────────────────────────────────
// Đếm ngược tới `endsAt` theo giờ server, cập nhật mỗi giây. Hết giờ không khoá ô soạn (server không tự nộp),
// chỉ chuyển sang "Quá thời gian +mm:ss" màu đỏ.

export function ExamClock({ endsAt }: { endsAt: string }) {
  const [now, setNow] = useState(serverNow);

  useEffect(() => {
    const timer = setInterval(() => setNow(serverNow()), 1_000);
    return () => clearInterval(timer);
  }, []);

  const { text, overtime, urgent } = formatExamClock(endsAt, now);
  return (
    <span
      role="timer"
      aria-live="off"
      className={clsx(
        'inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-small font-semibold tabular-nums',
        overtime ? 'bg-error-bg text-error' : urgent ? 'bg-warning-bg text-warning' : 'bg-brand-50 text-brand-700'
      )}
    >
      {overtime ? <TimerOff size={14} /> : <Timer size={14} />}
      {overtime ? `Quá thời gian ${text}` : `Còn ${text}`}
    </span>
  );
}
