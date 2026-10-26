'use client';

import { useEffect, useRef } from 'react';

import clsx from 'clsx';

import type { DiffOp } from '@/lib/utils/textDiff';

// ─── Nội dung so sánh 2 lần nộp: Gộp (chữ bỏ gạch đỏ, chữ thêm nền xanh) / Song song (bài cũ | bài mới) ─
// Đoạn trích của lỗi đang chọn (theo offset bài cũ) được viền và cuộn tới. Chỉ là chữ thường, không innerHTML.

const ESSAY = 'font-serif text-[16px]/[28px] whitespace-pre-wrap text-neutral-900 sm:text-essay';
const REMOVED = 'rounded-sm bg-diff-removed text-diff-removed-text line-through';
const ADDED = 'rounded-sm bg-diff-added text-diff-added-text no-underline';
const MARK = 'rounded-sm outline-2 outline-offset-1 outline-brand-500';

type Range = { start: number; end: number };

/** Chữ bên cũ của một đoạn, cắt ra phần nằm trong đoạn trích đang chọn để viền. */
function OldText({
  op,
  mark,
  markRef
}: {
  op: DiffOp;
  mark: Range | null;
  markRef: React.RefObject<HTMLSpanElement | null>;
}) {
  if (!mark || mark.end <= op.oldStart || mark.start >= op.oldEnd) return <>{op.text}</>;
  const s = Math.max(mark.start, op.oldStart) - op.oldStart;
  const e = Math.min(mark.end, op.oldEnd) - op.oldStart;
  return (
    <>
      {op.text.slice(0, s)}
      <span ref={mark.start >= op.oldStart ? markRef : undefined} className={MARK}>
        {op.text.slice(s, e)}
      </span>
      {op.text.slice(e)}
    </>
  );
}

function Sheet({ title, children }: { title?: string; children: React.ReactNode }) {
  return (
    <div className="min-w-0 rounded-lg border border-neutral-100 bg-neutral-0 shadow-sm">
      {title && (
        <div className="border-b border-neutral-100 px-4 py-2 text-small font-semibold text-neutral-700">{title}</div>
      )}
      <div className={clsx('px-4 py-5 sm:px-6', ESSAY)}>{children}</div>
    </div>
  );
}

export function VersionDiff({
  ops,
  view,
  oldLabel,
  newLabel,
  mark
}: {
  ops: DiffOp[];
  view: 'merged' | 'split';
  oldLabel: string;
  newLabel: string;
  mark: Range | null;
}) {
  const markRef = useRef<HTMLSpanElement | null>(null);

  useEffect(() => {
    markRef.current?.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }, [mark, view]);

  if (view === 'merged') {
    return (
      <Sheet>
        {ops.map((op, i) =>
          op.type === 'added' ? (
            <ins key={i} className={ADDED}>
              {op.text}
            </ins>
          ) : (
            <span key={i} className={op.type === 'removed' ? REMOVED : undefined}>
              <OldText op={op} mark={mark} markRef={markRef} />
            </span>
          )
        )}
      </Sheet>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-3 xl:grid-cols-2">
      <Sheet title={oldLabel}>
        {ops
          .filter((op) => op.type !== 'added')
          .map((op, i) => (
            <span
              key={i}
              className={op.type === 'removed' ? 'rounded-sm bg-diff-removed text-diff-removed-text' : undefined}
            >
              <OldText op={op} mark={mark} markRef={markRef} />
            </span>
          ))}
      </Sheet>
      <Sheet title={newLabel}>
        {ops
          .filter((op) => op.type !== 'removed')
          .map((op, i) => (
            <span key={i} className={op.type === 'added' ? 'rounded-sm bg-diff-added text-diff-added-text' : undefined}>
              {op.text}
            </span>
          ))}
      </Sheet>
    </div>
  );
}
