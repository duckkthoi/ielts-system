'use client';

import { useEffect, useState } from 'react';

import { Popover, Skeleton } from 'antd';
import clsx from 'clsx';
import { ExternalLink, Info } from 'lucide-react';

import { assignmentApi } from '@/lib/api/assignment';
import type { BandDescriptors, CriterionCode, TaskType } from '@/lib/types/assignment';

// ─── Bảng mô tả band thu gọn của một tiêu chí (docs/07 mục 7.2, docs/08 mục 5.4) ─
// Bấm ⓘ cạnh tên tiêu chí → mỗi band 9 → 0 một dòng diễn giải ngắn tiếng Việt (không phải văn bản chính
// thức), dòng ứng với điểm đang chọn tô brand-50, cuối có link bản chính thức. Dữ liệu tĩnh → tải một lần
// mỗi loại phần rồi giữ trong bộ nhớ.

const cache = new Map<string, Promise<BandDescriptors>>();

function loadDescriptors(taskType: TaskType): Promise<BandDescriptors> {
  let pending = cache.get(taskType);
  if (!pending) {
    pending = assignmentApi.bandDescriptors(taskType);
    pending.catch(() => cache.delete(taskType));
    cache.set(taskType, pending);
  }
  return pending;
}

function DescriptorTable({ code, taskType, score }: { code: CriterionCode; taskType: TaskType; score: number | null }) {
  const [data, setData] = useState<BandDescriptors | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    loadDescriptors(taskType)
      .then((d) => {
        if (!cancelled) setData(d);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, [taskType]);

  if (failed) return <p className="text-small text-neutral-500">Không tải được bảng mô tả.</p>;
  if (!data) return <Skeleton active paragraph={{ rows: 5 }} title={false} />;

  const criterion = data.criteria[code];
  const bands = criterion ? Object.entries(criterion.bands).sort(([a], [b]) => Number(b) - Number(a)) : [];
  const current = score === null ? null : Math.floor(score);

  return (
    <div className="flex max-h-[60vh] w-[min(380px,85vw)] flex-col gap-2">
      {bands.length === 0 ? (
        <p className="text-small text-neutral-500">Chưa có mô tả cho tiêu chí này.</p>
      ) : (
        <ol className="m-0 flex list-none flex-col gap-0.5 overflow-y-auto p-0">
          {bands.map(([band, text]) => (
            <li
              key={band}
              className={clsx(
                'flex gap-2.5 rounded-md px-2 py-1 text-small',
                Number(band) === current ? 'bg-brand-50 text-neutral-900' : 'text-neutral-700'
              )}
            >
              <span className="w-4 shrink-0 text-right font-semibold tabular-nums">{band}</span>
              <span>{text}</span>
            </li>
          ))}
        </ol>
      )}
      <p className="text-caption text-neutral-400">{data.note}</p>
      <a
        href={data.source.url}
        target="_blank"
        rel="noopener noreferrer"
        className="inline-flex items-center gap-1 text-small"
      >
        Bản chính thức (ielts.org) <ExternalLink size={12} />
      </a>
    </div>
  );
}

export function DescriptorPanel({
  code,
  name,
  taskType,
  score
}: {
  code: CriterionCode;
  name: string;
  taskType: TaskType;
  score: number | null;
}) {
  return (
    <Popover
      trigger="click"
      placement="leftTop"
      title={`${code} · ${name}`}
      content={<DescriptorTable code={code} taskType={taskType} score={score} />}
      destroyOnHidden
    >
      <button
        type="button"
        className="ml-1 inline-flex cursor-pointer align-middle text-neutral-400 hover:text-brand-600"
        aria-label={`Mô tả các band của ${name}`}
      >
        <Info size={15} />
      </button>
    </Popover>
  );
}
