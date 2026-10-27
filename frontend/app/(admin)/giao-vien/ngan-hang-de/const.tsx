import { Button, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Eye, Pencil, Send, Trash2 } from 'lucide-react';

import { TASK_TYPE_LABEL } from '@/lib/constants';
import type { QuestionBankSummary } from '@/lib/types/questionBank';
import type { TaskType } from '@/lib/types/assignment';
import { formatDateTime } from '@/lib/utils/date';

export const PAGE_SIZE = 20;

export const TASK_FILTER: { value: TaskType | 'all'; label: string }[] = [
  { value: 'all', label: 'Tất cả' },
  { value: 'WRITING_TASK1', label: 'Task 1' },
  { value: 'WRITING_TASK2', label: 'Task 2' },
  { value: 'CUSTOM', label: 'Tự do' }
];

export const TASK_OPTIONS: { value: TaskType; label: string }[] = [
  { value: 'WRITING_TASK1', label: TASK_TYPE_LABEL.WRITING_TASK1 },
  { value: 'WRITING_TASK2', label: TASK_TYPE_LABEL.WRITING_TASK2 },
  { value: 'CUSTOM', label: TASK_TYPE_LABEL.CUSTOM }
];

export const SORT_OPTIONS = [
  { value: 'updatedAt,desc', label: 'Mới cập nhật' },
  { value: 'createdAt,desc', label: 'Mới tạo' },
  { value: 'title,asc', label: 'Tên A–Z' }
];

interface ColumnActions {
  onView: (id: number) => void;
  onAssign: (id: number) => void;
  onEdit: (id: number) => void;
  onDelete: (item: QuestionBankSummary) => void;
  onTag: (tag: string) => void;
}

export function buildColumns({
  onView,
  onAssign,
  onEdit,
  onDelete,
  onTag
}: ColumnActions): ColumnsType<QuestionBankSummary> {
  return [
    {
      title: 'Tên đề',
      key: 'title',
      render: (_, item) => (
        <div className="min-w-60">
          <Button type="link" className="h-auto p-0 font-semibold" onClick={() => onView(item.id)}>
            {item.title}
          </Button>
          {item.tags.length > 0 && (
            <div className="mt-1 flex flex-wrap gap-1">
              {item.tags.map((tag) => (
                <Tag key={tag} className="m-0 cursor-pointer" onClick={() => onTag(tag)}>
                  {tag}
                </Tag>
              ))}
            </div>
          )}
        </div>
      )
    },
    {
      title: 'Loại đề',
      dataIndex: 'taskType',
      width: 190,
      render: (taskType: TaskType) => TASK_TYPE_LABEL[taskType]
    },
    {
      title: 'Yêu cầu',
      key: 'requirements',
      width: 170,
      responsive: ['md'],
      render: (_, item) => (
        <span className="text-small text-neutral-600">
          {item.minWords !== null ? `${item.minWords} từ` : 'Không giới hạn từ'}
          {item.suggestedMinutes !== null && ` · ${item.suggestedMinutes} phút`}
        </span>
      )
    },
    {
      title: 'Cập nhật',
      dataIndex: 'updatedAt',
      width: 160,
      responsive: ['lg'],
      render: (updatedAt: string) => <span className="text-small tabular-nums">{formatDateTime(updatedAt)}</span>
    },
    {
      title: '',
      key: 'actions',
      width: 245,
      align: 'right',
      fixed: 'right',
      render: (_, item) => (
        <span className="inline-flex items-center gap-1">
          <Button type="primary" size="small" icon={<Send size={14} />} onClick={() => onAssign(item.id)}>
            Giao bài
          </Button>
          <Button
            type="text"
            size="small"
            icon={<Eye size={15} />}
            onClick={() => onView(item.id)}
            aria-label="Xem đề"
          />
          <Button
            type="text"
            size="small"
            icon={<Pencil size={15} />}
            onClick={() => onEdit(item.id)}
            aria-label="Sửa đề"
          />
          <Button
            danger
            type="text"
            size="small"
            icon={<Trash2 size={15} />}
            onClick={() => onDelete(item)}
            aria-label="Xoá đề"
          />
        </span>
      )
    }
  ];
}
