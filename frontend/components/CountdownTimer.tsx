'use client';

import { useEffect, useState } from 'react';

import clsx from 'clsx';
import { Clock } from 'lucide-react';

import { describeDue } from '@/lib/utils/due';
import type { DueTone } from '@/lib/utils/due';

// ─── Thời gian còn lại tới hạn (docs/07 mục 7.2) ──────────────────────────────
// Tự cập nhật mỗi giây khi còn ≤ 24 giờ, mỗi phút khi còn xa. Chỉ để hiển thị; server quyết định quá hạn.
// TODO(F4): bù độ lệch giờ máy theo GET /api/time (SD-11) — ghi ở PROGRESS "Tồn đọng".

const TONE_CLASS: Record<DueTone, string> = {
  normal: 'bg-neutral-50 text-neutral-700',
  warning: 'bg-warning-bg text-warning',
  error: 'bg-error-bg text-error',
  overdue: 'bg-error-bg text-error'
};

export function CountdownTimer({ dueAt }: { dueAt: string }) {
  const [now, setNow] = useState(() => Date.now());
  const left = Date.parse(dueAt) - now;

  useEffect(() => {
    const interval = left > 0 && left <= 24 * 3_600_000 ? 1_000 : 60_000;
    const timer = setInterval(() => setNow(Date.now()), interval);
    return () => clearInterval(timer);
  }, [left]);

  const { text, tone } = describeDue(dueAt, now);
  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1 rounded-full px-2 py-px text-caption font-medium tabular-nums',
        TONE_CLASS[tone]
      )}
    >
      <Clock size={12} />
      {text}
    </span>
  );
}
