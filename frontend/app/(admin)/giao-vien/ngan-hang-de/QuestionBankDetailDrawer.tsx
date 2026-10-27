'use client';

import { useEffect, useState } from 'react';

import { Button, Collapse, Descriptions, Empty, Image, Tag } from 'antd';
import { Pencil, Send } from 'lucide-react';

import { AppDrawer } from '@/components/AppDrawer';
import { TASK_TYPE_LABEL } from '@/lib/constants';
import { questionBankApi } from '@/lib/api/questionBank';
import type { QuestionBankDetail } from '@/lib/types/questionBank';
import { formatDateTime } from '@/lib/utils/date';

interface QuestionBankDetailDrawerProps {
  itemId: number | null;
  onClose: () => void;
  onAssign: (id: number) => void;
  onEdit: (id: number) => void;
}

export function QuestionBankDetailDrawer({ itemId, onClose, onAssign, onEdit }: QuestionBankDetailDrawerProps) {
  const [result, setResult] = useState<{ itemId: number; detail: QuestionBankDetail | null } | null>(null);
  const detail = result?.itemId === itemId ? result.detail : null;
  const loading = itemId !== null && result?.itemId !== itemId;

  useEffect(() => {
    if (itemId === null) return;
    let cancelled = false;
    void questionBankApi
      .detail(itemId)
      .then((data) => {
        if (!cancelled) setResult({ itemId, detail: data });
      })
      .catch(() => {
        if (!cancelled) setResult({ itemId, detail: null });
      });
    return () => {
      cancelled = true;
    };
  }, [itemId]);

  return (
    <AppDrawer
      open={itemId !== null}
      title={detail?.title ?? 'Chi tiết đề'}
      subtitle={detail ? TASK_TYPE_LABEL[detail.taskType] : undefined}
      size={720}
      loading={loading}
      onClose={onClose}
      footerActions={
        <>
          <Button onClick={onClose}>Đóng</Button>
          {detail && (
            <>
              <Button icon={<Pencil size={15} />} onClick={() => onEdit(detail.id)}>
                Sửa đề
              </Button>
              <Button type="primary" icon={<Send size={15} />} onClick={() => onAssign(detail.id)}>
                Giao bài
              </Button>
            </>
          )}
        </>
      }
    >
      {!loading && !detail ? (
        <Empty description="Không tải được chi tiết đề" />
      ) : detail ? (
        <div className="flex flex-col gap-5">
          <Descriptions column={2} size="small" bordered>
            <Descriptions.Item label="Số từ tối thiểu">{detail.minWords ?? 'Không đặt'}</Descriptions.Item>
            <Descriptions.Item label="Thời gian gợi ý">
              {detail.suggestedMinutes !== null ? `${detail.suggestedMinutes} phút` : 'Không đặt'}
            </Descriptions.Item>
            <Descriptions.Item label="Tạo lúc">{formatDateTime(detail.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="Cập nhật">{formatDateTime(detail.updatedAt)}</Descriptions.Item>
          </Descriptions>

          {detail.tags.length > 0 && (
            <div>
              <p className="mb-2 text-small font-semibold text-neutral-700">Tag</p>
              <div className="flex flex-wrap gap-1.5">
                {detail.tags.map((tag) => (
                  <Tag key={tag} className="m-0">
                    {tag}
                  </Tag>
                ))}
              </div>
            </div>
          )}

          <section>
            <h3 className="mb-2 font-semibold text-neutral-900">Nội dung đề</h3>
            <div
              className="rounded-md border border-neutral-100 bg-neutral-25 p-4 font-serif text-prompt text-neutral-900 [&_ol]:list-decimal [&_ol]:pl-5 [&_p]:mb-2 [&_ul]:list-disc [&_ul]:pl-5"
              dangerouslySetInnerHTML={{ __html: detail.prompt }}
            />
            {detail.imageUrl && <Image className="mt-3 rounded-md" src={detail.imageUrl} alt="Ảnh đề bài" />}
          </section>

          <section>
            <h3 className="mb-2 font-semibold text-neutral-900">Rubric và checklist</h3>
            {detail.criteria.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Đề này chưa có tiêu chí chấm" />
            ) : (
              <Collapse
                items={detail.criteria.map((criterion) => ({
                  key: criterion.id,
                  label: (
                    <span>
                      <strong>
                        {criterion.code ? `${criterion.code} · ` : ''}
                        {criterion.name}
                      </strong>
                      <span className="ml-2 text-small font-normal text-neutral-500">
                        Tối đa {criterion.maxScore} · bước {criterion.step}
                      </span>
                    </span>
                  ),
                  children: (
                    <div>
                      {criterion.description && <p className="mb-3 text-neutral-600">{criterion.description}</p>}
                      {criterion.checklist.length === 0 ? (
                        <p className="text-small text-neutral-500">Chưa có checklist</p>
                      ) : (
                        <ul className="flex list-disc flex-col gap-2 pl-5">
                          {criterion.checklist.map((item) => (
                            <li key={item.id}>
                              {item.content}
                              {item.kind === 'LIMIT' && (
                                <Tag className="ml-2" color="orange">
                                  Tối đa {item.maxBand}
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
      ) : null}
    </AppDrawer>
  );
}
