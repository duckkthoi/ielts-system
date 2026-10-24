import clsx from 'clsx';

// ─── "248 / 250 từ" — chưa đủ tối thiểu: warning, đủ: success (docs/07 mục 7.2) ─
// Số từ đếm bằng lib/utils/wordCount.ts, cùng thuật toán với server (BR-20).

export function WordCounter({ count, minWords }: { count: number; minWords: number | null }) {
  if (!minWords) return <span className="text-small font-medium text-neutral-700 tabular-nums">{count} từ</span>;
  return (
    <span
      className={clsx('text-small font-medium tabular-nums', count < minWords ? 'text-warning' : 'text-success')}
      title={count < minWords ? `Còn thiếu ${minWords - count} từ` : 'Đã đủ số từ tối thiểu'}
    >
      {count} / {minWords} từ
    </span>
  );
}
