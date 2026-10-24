'use client';

import { useEffect, useState } from 'react';

import { Empty, Skeleton, Tag } from 'antd';

import { AppDrawer } from '@/components/AppDrawer';
import { BandBadge } from '@/components/BandBadge';
import { studentApi } from '@/lib/api/student';
import type { StudentPartDetail, VersionSummary } from '@/lib/types/student';
import { formatDateTime } from '@/lib/utils/date';

// ─── Lịch sử nộp một phần: mỗi lần nộp, số từ, muộn, band nếu đã trả (BR-13) ───
// Xem nội dung + kết quả từng version và so sánh 2 version làm ở F6 (Prompt 7 D, E).

export function VersionHistoryDrawer({ part, onClose }: { part: StudentPartDetail | null; onClose: () => void }) {
  const [loaded, setLoaded] = useState<{ partId: number; items: VersionSummary[] } | null>(null);
  const partId = part?.partId ?? null;

  useEffect(() => {
    if (partId === null) return;
    let cancelled = false;
    studentApi
      .versions(partId)
      .then((items) => {
        if (!cancelled) setLoaded({ partId, items });
      })
      .catch(() => {
        if (!cancelled) setLoaded({ partId, items: [] });
      });
    return () => {
      cancelled = true;
    };
  }, [partId]);

  const items = loaded && loaded.partId === partId ? loaded.items : null;

  return (
    <AppDrawer open={part !== null} onClose={onClose} size={440} title="Lịch sử nộp" subtitle={part?.title}>
      {!items ? (
        <Skeleton active paragraph={{ rows: 4 }} />
      ) : items.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Bạn chưa nộp phần này lần nào." />
      ) : (
        <ol className="m-0 flex list-none flex-col gap-3 p-0">
          {items.map((v) => (
            <li
              key={v.versionNo}
              className="flex items-center justify-between gap-3 rounded-md border border-neutral-100 px-4 py-3"
            >
              <div className="min-w-0">
                <p className="font-semibold text-neutral-900">
                  Lần nộp {v.versionNo}
                  {v.late && (
                    <Tag color="orange" className="ml-2">
                      Nộp muộn
                    </Tag>
                  )}
                </p>
                <p className="text-small text-neutral-500 tabular-nums">
                  {formatDateTime(v.submittedAt)} · {v.wordCount} từ
                </p>
                {v.returned && v.returnedAt && (
                  <p className="text-small text-success">Đã trả bài lúc {formatDateTime(v.returnedAt)}</p>
                )}
              </div>
              {v.returned ? (
                <BandBadge band={v.band} />
              ) : (
                <span className="shrink-0 text-small text-neutral-400">Chờ kết quả</span>
              )}
            </li>
          ))}
        </ol>
      )}
    </AppDrawer>
  );
}
