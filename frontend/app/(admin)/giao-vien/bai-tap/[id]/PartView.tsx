'use client';

import { Collapse, Empty, Image, Tag } from 'antd';
import { Clock, FileText, Lock } from 'lucide-react';

import { fileUrl, TASK_TYPE_LABEL } from '@/lib/constants';
import type { PartDto } from '@/lib/types/assignment';
import { formatDateTime } from '@/lib/utils/date';

// ─── Một phần của bài: đề, ảnh, yêu cầu, rubric (tiêu chí + checklist Đạt / Giới hạn điểm) ─
// `prompt` là HTML đã được server lọc — trường duy nhất được render bằng dangerouslySetInnerHTML.

const formatScore = (n: number) => (Number.isInteger(n) ? String(n) : n.toFixed(1));

export function PartView({ part }: { part: PartDto }) {
  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap gap-2 text-small">
        <Tag color="blue">{TASK_TYPE_LABEL[part.taskType]}</Tag>
        {part.minWords ? (
          <span className="inline-flex items-center gap-1 text-neutral-500">
            <FileText size={14} /> Tối thiểu {part.minWords} từ
          </span>
        ) : null}
        {part.suggestedMinutes ? (
          <span className="inline-flex items-center gap-1 text-neutral-500">
            <Clock size={14} /> Gợi ý {part.suggestedMinutes} phút
          </span>
        ) : null}
        {part.partDueAt && <span className="text-warning">Hạn riêng: {formatDateTime(part.partDueAt)}</span>}
        {part.weight !== null && <span className="text-neutral-500">· Trọng số {formatScore(part.weight)}</span>}
      </div>

      <section>
        <h4 className="mb-2 text-caption font-semibold tracking-wide text-neutral-500 uppercase">Đề bài</h4>
        <div
          className="rounded-md border border-neutral-100 bg-neutral-25 p-4 font-serif text-prompt text-neutral-900 [&_ol]:list-decimal [&_ol]:pl-5 [&_p]:mb-2 [&_ul]:list-disc [&_ul]:pl-5"
          dangerouslySetInnerHTML={{ __html: part.prompt }}
        />
        {part.imageUrl && (
          <Image
            src={fileUrl(part.imageUrl)}
            alt={`Hình minh hoạ đề: ${part.title}`}
            className="mt-3 max-h-80 rounded-md border border-neutral-100 object-contain"
          />
        )}
      </section>

      <section>
        <h4 className="mb-2 text-caption font-semibold tracking-wide text-neutral-500 uppercase">
          Tiêu chí chấm ({part.criteria.length})
        </h4>
        {part.criteria.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Phần này chưa có tiêu chí — cần có trước khi phát hành"
          />
        ) : (
          <Collapse
            size="small"
            items={part.criteria.map((c) => ({
              key: c.id,
              label: (
                <span className="font-medium text-neutral-900">
                  {c.code && <span className="mr-2 font-mono text-brand-600">{c.code}</span>}
                  {c.name}
                </span>
              ),
              extra: (
                <span className="text-small text-neutral-500">
                  Tối đa {formatScore(c.maxScore)} · bước {formatScore(c.step)} · ×{formatScore(c.weight)}
                </span>
              ),
              children: (
                <div className="flex flex-col gap-2">
                  {c.description && <p className="text-small text-neutral-500">{c.description}</p>}
                  {c.checklist.length === 0 ? (
                    <p className="text-small text-neutral-400">Không có checklist</p>
                  ) : (
                    <ul className="m-0 flex list-none flex-col gap-1.5 p-0">
                      {c.checklist.map((item) => (
                        <li key={item.id} className="flex items-start justify-between gap-3">
                          <span className="flex items-start gap-2">
                            <span
                              className={`mt-1.5 size-1.5 shrink-0 rounded-full ${item.kind === 'LIMIT' ? 'bg-warning' : 'bg-success'}`}
                            />
                            {item.content}
                          </span>
                          {item.kind === 'LIMIT' && item.maxBand !== null && (
                            <Tag color="warning" className="m-0 inline-flex shrink-0 items-center gap-1">
                              <Lock size={11} /> Tối đa {formatScore(item.maxBand)}
                            </Tag>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )
            }))}
          />
        )}
      </section>
    </div>
  );
}
