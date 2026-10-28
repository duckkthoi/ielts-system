'use client';

import { useEffect, useState } from 'react';

import { App, Button, Empty, Input, List, Modal, Pagination, Select, Skeleton, Tag } from 'antd';
import { BookOpen, Search } from 'lucide-react';

import { questionBankApi } from '@/lib/api/questionBank';
import { TASK_TYPE_LABEL } from '@/lib/constants';
import type { QuestionBankDetail, QuestionBankSummary } from '@/lib/types/questionBank';
import type { TaskType } from '@/lib/types/assignment';

import { TASK_TYPE_OPTIONS } from './const';

const PAGE_SIZE = 6;

interface QuestionBankPickerModalProps {
  open: boolean;
  taskType?: TaskType;
  onClose: () => void;
  onPick: (item: QuestionBankDetail) => void;
}

/** Chọn một đề, sau đó tải chi tiết để sao chép toàn bộ đề và rubric vào phần đang soạn. */
export function QuestionBankPickerModal({ open, taskType, onClose, onPick }: QuestionBankPickerModalProps) {
  const { message } = App.useApp();
  const [items, setItems] = useState<QuestionBankSummary[]>([]);
  const [keyword, setKeyword] = useState('');
  const [typeFilter, setTypeFilter] = useState<TaskType | undefined>(taskType);
  const [tag, setTag] = useState<string | undefined>();
  const [page, setPage] = useState(0);
  const [requestNo, setRequestNo] = useState(0);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [pickingId, setPickingId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    void questionBankApi
      .list({
        keyword: keyword || undefined,
        taskType: typeFilter,
        tag,
        page,
        size: PAGE_SIZE,
        sort: 'updatedAt,desc'
      })
      .then((response) => {
        if (cancelled) return;
        setItems(response.data);
        setTotal(response.totalRecord ?? response.data.length);
      })
      .catch(() => {
        if (cancelled) return;
        setItems([]);
        setTotal(0);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [keyword, page, requestNo, tag, typeFilter]);

  const pick = async (id: number) => {
    setPickingId(id);
    try {
      onPick(await questionBankApi.detail(id));
      onClose();
    } catch {
      message.error('Không tải được chi tiết đề');
    } finally {
      setPickingId(null);
    }
  };

  return (
    <Modal open={open} title="Chọn từ ngân hàng đề" width={760} footer={null} destroyOnHidden onCancel={onClose}>
      <div className="mb-4 grid gap-2 sm:grid-cols-[1fr_220px]">
        <Input.Search
          allowClear
          prefix={<Search size={14} className="text-neutral-400" />}
          placeholder="Tìm theo tên hoặc nội dung đề"
          onSearch={(value) => {
            setLoading(true);
            setKeyword(value.trim());
            setPage(0);
            setRequestNo((current) => current + 1);
          }}
        />
        <Select
          allowClear
          value={typeFilter}
          placeholder="Tất cả loại đề"
          options={TASK_TYPE_OPTIONS}
          onChange={(value) => {
            setLoading(true);
            setTypeFilter(value);
            setPage(0);
            setRequestNo((current) => current + 1);
          }}
        />
      </div>

      {tag && (
        <div className="mb-3 text-small text-neutral-600">
          Đang lọc tag:{' '}
          <Tag
            closable
            onClose={() => {
              setLoading(true);
              setTag(undefined);
              setRequestNo((current) => current + 1);
            }}
          >
            {tag}
          </Tag>
        </div>
      )}

      {loading ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : items.length === 0 ? (
        <Empty description="Chưa có đề phù hợp trong ngân hàng" />
      ) : (
        <List
          dataSource={items}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button
                  key="pick"
                  type="primary"
                  ghost
                  icon={<BookOpen size={14} />}
                  loading={pickingId === item.id}
                  onClick={() => void pick(item.id)}
                >
                  Dùng đề này
                </Button>
              ]}
            >
              <List.Item.Meta
                title={item.title}
                description={
                  <div className="flex flex-wrap items-center gap-1.5">
                    <span>{TASK_TYPE_LABEL[item.taskType]}</span>
                    {item.minWords !== null && <span>· {item.minWords} từ</span>}
                    {item.suggestedMinutes !== null && <span>· {item.suggestedMinutes} phút</span>}
                    {item.tags.map((itemTag) => (
                      <Tag
                        key={itemTag}
                        className="cursor-pointer"
                        onClick={() => {
                          setLoading(true);
                          setTag(itemTag);
                          setPage(0);
                          setRequestNo((current) => current + 1);
                        }}
                      >
                        {itemTag}
                      </Tag>
                    ))}
                  </div>
                }
              />
            </List.Item>
          )}
        />
      )}

      {total > PAGE_SIZE && (
        <div className="mt-4 flex justify-end">
          <Pagination
            current={page + 1}
            pageSize={PAGE_SIZE}
            total={total}
            showSizeChanger={false}
            onChange={(next) => {
              setLoading(true);
              setPage(next - 1);
              setRequestNo((current) => current + 1);
            }}
          />
        </div>
      )}
    </Modal>
  );
}
