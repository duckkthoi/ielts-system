'use client';

import { Button } from 'antd';
import clsx from 'clsx';

import type { ChatReader } from '@/lib/types/chat';
import type { ChatLine } from '@/lib/utils/chat';
import { seenBy } from '@/lib/utils/chat';
import { formatDate, formatTime, parseServerDate } from '@/lib/utils/date';

// ─── Các tin trong một kênh: ngăn theo ngày, bong bóng trái / phải ────────────
// Nội dung hiện dạng chữ thuần (whitespace-pre-wrap), không bao giờ dùng innerHTML.
// Dưới tin cuối cùng của mình đã được server lưu: "Đã xem" (kênh lớp ghi tên người xem).

function dayLabel(iso: string): string {
  const day = parseServerDate(iso).startOf('day');
  const today = parseServerDate(new Date()).startOf('day');
  const diff = today.diff(day, 'day');
  if (diff === 0) return 'Hôm nay';
  if (diff === 1) return 'Hôm qua';
  return formatDate(iso);
}

export function MessageList({
  lines,
  reads,
  myId,
  showNames,
  onRetry
}: {
  lines: ChatLine[];
  reads: ChatReader[];
  myId: number;
  /** Kênh lớp nhiều người → ghi tên người gửi trên bong bóng của người khác. */
  showNames: boolean;
  onRetry: (line: ChatLine) => void;
}) {
  const lastMine = [...lines].reverse().find((l) => l.senderId === myId && l.id !== null);
  const viewers = lastMine?.id ? seenBy(reads, lastMine.id, myId) : [];
  // Sự kiện READ không kèm tên: biết tên thì liệt kê, không thì ghi số người.
  const seenLabel = viewers.every(Boolean) ? `bởi ${viewers.join(', ')}` : `bởi ${viewers.length} người`;

  return (
    <ol className="m-0 flex list-none flex-col gap-1 p-0">
      {lines.map((line, index) => {
        const prev = lines[index - 1];
        const mine = line.senderId === myId;
        const newDay = !prev || dayLabel(prev.createdAt) !== dayLabel(line.createdAt);
        const firstOfGroup = newDay || !prev || prev.senderId !== line.senderId;
        return (
          <li key={line.key} className="flex flex-col">
            {newDay && (
              <span className="my-3 self-center rounded-full bg-neutral-50 px-3 py-0.5 text-caption text-neutral-500">
                {dayLabel(line.createdAt)}
              </span>
            )}
            <div className={clsx('flex max-w-[80%] flex-col', mine ? 'items-end self-end' : 'items-start self-start')}>
              {!mine && showNames && firstOfGroup && (
                <span className="mt-1 mb-0.5 px-1 text-caption font-medium text-neutral-500">{line.senderName}</span>
              )}
              <div
                className={clsx(
                  'rounded-2xl px-3.5 py-2 break-words whitespace-pre-wrap',
                  mine ? 'rounded-br-md bg-brand-600 text-neutral-0' : 'rounded-bl-md bg-neutral-50 text-neutral-900',
                  line.local === 'sending' && 'opacity-70',
                  line.local === 'failed' && 'bg-error-bg! text-error! ring-1 ring-error-border'
                )}
                title={line.local ? undefined : formatDate(line.createdAt) + ' ' + formatTime(line.createdAt)}
              >
                {line.content}
              </div>
              <span className="mt-0.5 px-1 text-caption text-neutral-400">
                {line.local === 'sending' ? (
                  'Đang gửi…'
                ) : line.local === 'failed' ? (
                  <span className="text-error">
                    Chưa gửi được ·{' '}
                    <Button
                      type="link"
                      size="small"
                      className="h-auto! p-0! text-caption!"
                      onClick={() => onRetry(line)}
                    >
                      Gửi lại
                    </Button>
                  </span>
                ) : (
                  <>
                    {formatTime(line.createdAt)}
                    {line === lastMine && viewers.length > 0 && (
                      <span className="ml-1.5 text-brand-600">· Đã xem{showNames ? ` ${seenLabel}` : ''}</span>
                    )}
                  </>
                )}
              </span>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
