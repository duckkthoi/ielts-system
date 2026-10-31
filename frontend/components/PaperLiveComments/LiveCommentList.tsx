'use client';

import { Button, Empty, Popconfirm, Result, Skeleton, Tag, Tooltip } from 'antd';
import clsx from 'clsx';
import { CheckCircle2, Pencil, RotateCcw, Trash2 } from 'lucide-react';

import { LIVE_COMMENT_KIND_META } from '@/lib/theme/status';
import type { LiveComment } from '@/lib/types/paper';
import { formatRelative } from '@/lib/utils/date';

// ─── Danh sách nhận xét trực tiếp (giáo viên + sinh viên, H4) ─────────────────
// Bấm thẻ → cuộn tới đoạn trên trình soạn; bấm đoạn đã tô → thẻ được chọn và cuộn vào tầm nhìn (`live-comment-{id}`).

export function LiveCommentList({
  comments,
  staleIds,
  activeId,
  loading,
  error,
  onRetry,
  onSelect,
  sectionTitle,
  canManage,
  onEdit,
  onDelete,
  onToggleResolved,
  emptyText
}: {
  comments: LiveComment[];
  staleIds: number[];
  activeId: number | null;
  loading: boolean;
  error: boolean;
  onRetry: () => void;
  onSelect: (id: number) => void;
  sectionTitle: (key: string | null) => string | undefined;
  canManage: (c: LiveComment) => boolean;
  onEdit?: (c: LiveComment) => void;
  onDelete?: (c: LiveComment) => void;
  onToggleResolved: (c: LiveComment) => void;
  emptyText: string;
}) {
  if (loading) return <Skeleton active paragraph={{ rows: 4 }} />;
  if (error && comments.length === 0) {
    return (
      <Result
        status="warning"
        title="Không tải được nhận xét"
        subTitle="Kiểm tra kết nối mạng."
        extra={<Button onClick={onRetry}>Thử lại</Button>}
      />
    );
  }
  if (comments.length === 0) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={emptyText} />;

  return (
    <ul className="m-0 flex list-none flex-col gap-2 p-0">
      {comments.map((c) => {
        const meta = LIVE_COMMENT_KIND_META[c.kind];
        const stale = staleIds.includes(c.id);
        const section = sectionTitle(c.sectionKey);
        return (
          <li
            key={c.id}
            id={`live-comment-${c.id}`}
            onClick={() => onSelect(c.id)}
            className={clsx(
              'cursor-pointer rounded-lg border bg-neutral-0 p-3 transition-colors',
              c.id === activeId ? 'border-brand-500 shadow-sm' : 'border-neutral-100 hover:border-neutral-300',
              c.resolvedAt && 'opacity-70'
            )}
          >
            <div className="mb-1.5 flex flex-wrap items-center gap-1.5">
              <Tag className="m-0" color={meta.tagColor}>
                {meta.label}
              </Tag>
              {section && <span className="text-caption text-neutral-500">{section}</span>}
              {c.resolvedAt && (
                <Tag className="m-0" color="success">
                  Đã xử lý
                </Tag>
              )}
            </div>
            <blockquote
              className={clsx(
                'm-0 mb-1.5 line-clamp-3 border-l-2 pl-2 font-serif text-small',
                stale
                  ? 'border-warning text-neutral-500 line-through'
                  : 'border-highlight-comment-line text-neutral-700'
              )}
            >
              {c.quote}
            </blockquote>
            {stale && <p className="m-0 mb-1 text-caption text-warning">Đoạn văn đã thay đổi</p>}
            <p className="m-0 text-body whitespace-pre-line text-neutral-900">{c.comment}</p>
            {c.suggestion && (
              <p className="m-0 mt-1 text-small whitespace-pre-line text-success">Gợi ý sửa: {c.suggestion}</p>
            )}
            <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
              <span className="text-caption text-neutral-500">
                {c.authorName} · {formatRelative(c.createdAt)}
                {c.resolvedByName && ` · ${c.resolvedByName} đã xử lý`}
              </span>
              <span className="flex gap-1" onClick={(e) => e.stopPropagation()}>
                <Tooltip title={c.resolvedAt ? 'Mở lại' : 'Đánh dấu đã xử lý'}>
                  <Button
                    size="small"
                    type="text"
                    aria-label={c.resolvedAt ? 'Mở lại' : 'Đánh dấu đã xử lý'}
                    icon={c.resolvedAt ? <RotateCcw size={14} /> : <CheckCircle2 size={14} />}
                    onClick={() => onToggleResolved(c)}
                  />
                </Tooltip>
                {canManage(c) && onEdit && (
                  <Tooltip title="Sửa">
                    <Button
                      size="small"
                      type="text"
                      aria-label="Sửa"
                      icon={<Pencil size={14} />}
                      onClick={() => onEdit(c)}
                    />
                  </Tooltip>
                )}
                {canManage(c) && onDelete && (
                  <Popconfirm title="Xoá nhận xét này?" okText="Xoá" cancelText="Huỷ" onConfirm={() => onDelete(c)}>
                    <Button size="small" type="text" danger aria-label="Xoá" icon={<Trash2 size={14} />} />
                  </Popconfirm>
                )}
              </span>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
