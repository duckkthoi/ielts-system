'use client';

import { Card, Empty } from 'antd';
import clsx from 'clsx';

import { EXAM_EVENT_META } from '@/lib/theme/status';
import type { ExamPush } from '@/lib/types/exam';
import { formatTime } from '@/lib/utils/date';

// ─── Bảng tin sự kiện thi mới nhất (BR-25) ────────────────────────────────────
// Mở trang nạp sẵn 50 sự kiện gần nhất, sau đó thêm sự kiện mới lên đầu. Lịch sử đầy đủ của từng phiên xem ở
// dòng thời gian. "Gửi bù" = xảy ra lúc máy sinh viên mất mạng, máy báo lại khi có mạng (giờ là lúc xảy ra).

export function EventFeed({ feed, onOpen }: { feed: ExamPush[]; onOpen: (push: ExamPush) => void }) {
  return (
    <Card title="Sự kiện mới" size="small" styles={{ body: { padding: 0 } }}>
      {feed.length === 0 ? (
        <Empty
          className="py-6"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="Rời trang, dán văn bản, mất kết nối… sẽ hiện ở đây ngay khi xảy ra."
        />
      ) : (
        <ul className="max-h-[60vh] divide-y divide-neutral-100 overflow-y-auto">
          {feed.map((e) => {
            const meta = EXAM_EVENT_META[e.type];
            return (
              <li key={`${e.sessionId}-${e.type}-${e.at}`}>
                <button
                  type="button"
                  onClick={() => onOpen(e)}
                  className={clsx(
                    'flex w-full items-start gap-2 px-3 py-2 text-left hover:bg-neutral-25',
                    meta.alert && 'bg-warning-bg/40'
                  )}
                >
                  <span className="mt-0.5 shrink-0 text-caption text-neutral-500 tabular-nums">{formatTime(e.at)}</span>
                  <span className="min-w-0 text-small">
                    <span className="font-medium text-neutral-900">{e.studentName}</span>{' '}
                    <span className={meta.alert ? 'text-warning' : 'text-neutral-700'}>
                      {meta.label.toLowerCase()}
                      {e.type === 'PASTE' && e.pasteChars !== null ? ` (${e.pasteChars} ký tự)` : ''}
                    </span>
                    {e.reportedLate && <span className="ml-1 text-caption text-neutral-500">· gửi bù</span>}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}
