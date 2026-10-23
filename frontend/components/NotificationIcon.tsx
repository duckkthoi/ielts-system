import { Bell, BookOpen, ClipboardCheck, Clock, Inbox, PencilLine } from 'lucide-react';

import { NOTIFICATION_TYPE_META } from '@/lib/theme/status';
import type { NotificationType } from '@/lib/types/notification';

// ─── Ô icon theo loại thông báo (chuông header + trang Thông báo) ─────────────

const ICONS: Record<NotificationType, typeof Bell> = {
  ASSIGNMENT_PUBLISHED: BookOpen,
  SUBMISSION_RECEIVED: Inbox,
  GRADE_RETURNED: ClipboardCheck,
  REVISION_REQUESTED: PencilLine,
  DEADLINE_REMINDER: Clock,
  MANUAL_REMINDER: Bell
};

export function NotificationIcon({ type, size = 36 }: { type: NotificationType; size?: number }) {
  const Icon = ICONS[type] ?? Bell;
  const meta = NOTIFICATION_TYPE_META[type];
  return (
    <span
      aria-label={meta?.label}
      className="grid shrink-0 place-items-center rounded-md"
      style={{ width: size, height: size, color: meta?.text, background: meta?.bg }}
    >
      <Icon size={Math.round(size / 2.2)} />
    </span>
  );
}
