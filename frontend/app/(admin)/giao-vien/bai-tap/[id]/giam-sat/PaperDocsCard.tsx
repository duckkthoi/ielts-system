'use client';

import { useCallback, useEffect, useState } from 'react';

import { Badge, Button, Card, Empty, Result, Skeleton } from 'antd';
import { Eye, RefreshCw } from 'lucide-react';

import { paperLiveCommentApi } from '@/lib/api/paperLiveComment';
import type { SectionDto } from '@/lib/types/assignment';
import type { TeacherPaperDocument } from '@/lib/types/paper';
import { formatRelative } from '@/lib/utils/date';

import { PaperLiveDrawer } from './PaperLiveDrawer';

// ─── Bài Paper: tài liệu các nhóm đang viết → xem trực tiếp + nhận xét (H4) ───
// Sửa gần nhất lên đầu; tự tải lại 30 giây một lần (số nhận xét chưa xử lý, lần sửa cuối).

const REFRESH_MS = 30_000;

export function PaperDocsCard({
  assignmentId,
  parts
}: {
  assignmentId: number;
  parts: { id: number; sections: SectionDto[] }[];
}) {
  const [docs, setDocs] = useState<TeacherPaperDocument[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [version, setVersion] = useState(0);
  const [target, setTarget] = useState<TeacherPaperDocument | null>(null);

  useEffect(() => {
    let cancelled = false;
    paperLiveCommentApi
      .teacherDocuments(assignmentId)
      .then((list) => {
        if (cancelled) return;
        setDocs(list);
        setFailed(false);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });
    const timer = setTimeout(() => setVersion((v) => v + 1), REFRESH_MS);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [assignmentId, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  const sections = parts.find((p) => p.id === target?.partId)?.sections ?? [];

  return (
    <Card
      className="mb-4"
      title="Tài liệu nhóm đang viết"
      extra={<Button size="small" type="text" icon={<RefreshCw size={14} />} onClick={reload} aria-label="Tải lại" />}
      styles={{ body: { padding: docs && docs.length ? 0 : undefined } }}
    >
      {failed && !docs ? (
        <Result
          status="warning"
          title="Không tải được danh sách tài liệu"
          extra={<Button onClick={reload}>Thử lại</Button>}
        />
      ) : !docs ? (
        <Skeleton active paragraph={{ rows: 3 }} />
      ) : docs.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa nhóm nào mở tài liệu để viết." />
      ) : (
        <ul className="m-0 list-none divide-y divide-neutral-100 p-0">
          {docs.map((d) => (
            <li key={d.documentId} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
              <span className="min-w-0">
                <span className="block font-medium text-neutral-900">
                  {d.groupName} · {d.partTitle}
                </span>
                <span className="block truncate text-caption text-neutral-500">
                  {d.members ?? 'Chưa có thành viên'} · sửa {formatRelative(d.updatedAt)}
                </span>
              </span>
              <span className="flex items-center gap-3">
                <Badge
                  count={d.openComments}
                  showZero={false}
                  title={`${d.openComments} nhận xét chưa xử lý / ${d.totalComments}`}
                />
                <Button size="small" icon={<Eye size={14} />} onClick={() => setTarget(d)}>
                  Xem &amp; nhận xét
                </Button>
              </span>
            </li>
          ))}
        </ul>
      )}
      <PaperLiveDrawer
        target={target}
        sections={sections}
        onClose={() => {
          setTarget(null);
          reload();
        }}
      />
    </Card>
  );
}
