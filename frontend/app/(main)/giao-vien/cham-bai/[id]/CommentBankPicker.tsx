'use client';

import { useCallback, useEffect, useState } from 'react';

import { App, Button, Empty, Input, Popover, Spin, Tag } from 'antd';
import { BookmarkPlus, Library, Trash2 } from 'lucide-react';

import { feedbackApi } from '@/lib/api/feedback';
import type { CommentBankDto, CriterionGroup } from '@/lib/types/grading';

// ─── Nhận xét mẫu (BR-22): chèn nhận xét hay dùng, lưu nhận xét đang viết, xoá mẫu của tôi ─
// Danh sách: của tôi (hay dùng lên đầu) rồi tới mẫu của hệ thống; lọc theo nhóm tiêu chí kèm mục dùng chung.

export function CommentBankPicker({
  group,
  current,
  onInsert
}: {
  group: CriterionGroup | null;
  current: string;
  onInsert: (text: string) => void;
}) {
  const { message } = App.useApp();
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [items, setItems] = useState<CommentBankDto[] | null>(null);
  const [tick, setTick] = useState(0);

  const reload = useCallback(() => setTick((t) => t + 1), []);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    feedbackApi
      .commentBank(group, keyword.trim())
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch(() => {
        if (!cancelled) setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, [open, group, keyword, tick]);

  const insert = (item: CommentBankDto) => {
    onInsert(item.content);
    setOpen(false);
    if (!item.system) void feedbackApi.useComment(item.id);
  };

  const saveCurrent = async () => {
    try {
      await feedbackApi.addComment({ criterion: group, content: current.trim() });
      message.success('Đã lưu vào nhận xét mẫu');
      reload();
    } catch {
      // axiosClient đã toast lý do
    }
  };

  const remove = async (item: CommentBankDto) => {
    try {
      await feedbackApi.deleteComment(item.id);
      reload();
    } catch {
      // axiosClient đã toast lý do
    }
  };

  const content = (
    <div className="flex w-80 flex-col gap-2">
      <Input.Search allowClear placeholder="Tìm nhận xét mẫu" onSearch={setKeyword} />
      <div className="max-h-64 overflow-y-auto">
        {!items ? (
          <div className="flex justify-center p-4">
            <Spin size="small" />
          </div>
        ) : items.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có nhận xét mẫu phù hợp" />
        ) : (
          <ul className="m-0 flex list-none flex-col gap-1 p-0">
            {items.map((item) => (
              <li key={item.id} className="group flex items-start gap-1 rounded-md hover:bg-neutral-50">
                <button
                  type="button"
                  onClick={() => insert(item)}
                  className="flex-1 cursor-pointer border-0 bg-transparent px-2 py-1.5 text-left text-small text-neutral-700"
                >
                  {item.content}
                  {item.system && (
                    <Tag className="ml-1" bordered={false}>
                      Mẫu
                    </Tag>
                  )}
                </button>
                {!item.system && (
                  <button
                    type="button"
                    aria-label="Xoá nhận xét mẫu"
                    onClick={() => void remove(item)}
                    className="mt-1.5 mr-1 cursor-pointer border-0 bg-transparent p-0 text-neutral-300 opacity-0 group-hover:opacity-100 hover:text-error"
                  >
                    <Trash2 size={13} />
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
      <Button
        size="small"
        icon={<BookmarkPlus size={14} />}
        disabled={!current.trim()}
        onClick={() => void saveCurrent()}
      >
        Lưu nhận xét đang viết vào mẫu
      </Button>
    </div>
  );

  return (
    <Popover open={open} onOpenChange={setOpen} trigger="click" placement="bottomLeft" content={content}>
      <Button size="small" type="link" icon={<Library size={14} />} className="px-0">
        Nhận xét mẫu
      </Button>
    </Popover>
  );
}
