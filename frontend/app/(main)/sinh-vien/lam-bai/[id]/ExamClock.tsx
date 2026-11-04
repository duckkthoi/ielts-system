'use client';

import { useEffect, useRef, useState } from 'react';

import clsx from 'clsx';
import { Timer, TimerOff } from 'lucide-react';

import { formatExamClock } from '@/lib/utils/exam';
import { serverNow } from '@/lib/utils/serverClock';

// ─── Đồng hồ chế độ thi (BR-23, BR-24) ────────────────────────────────────────
// Đếm ngược tới `endsAt` theo giờ server, cập nhật mỗi giây. Hết giờ gọi tự nộp một lần.

export function ExamClock({ endsAt, onExpired }: { endsAt: string; onExpired: () => void }) {
  const [now, setNow] = useState(serverNow);
  const expiredRef = useRef(false);

  useEffect(() => {
    const timer = setInterval(() => setNow(serverNow()), 1_000);
    return () => clearInterval(timer);
  }, []);

  const { text, overtime, urgent } = formatExamClock(endsAt, now);

  useEffect(() => {
    if (overtime && !expiredRef.current) {
      expiredRef.current = true;
      onExpired();
    }
  }, [onExpired, overtime]);
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
