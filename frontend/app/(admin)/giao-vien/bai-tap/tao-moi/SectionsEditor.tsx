'use client';

import { Button, Form, Input, InputNumber, Switch, Tooltip } from 'antd';
import type { FormRule } from 'antd';
import { ArrowDown, ArrowUp, Plus, Trash2 } from 'lucide-react';

import { newSection } from './paperTemplates';

// ─── Khung mục của một giai đoạn bài Paper (BR-29) ────────────────────────────
// Mỗi mục: tiêu đề, bắt buộc hay không, số từ tối thiểu / tối đa (chỉ cảnh báo khi viết). Mã mục server tự sinh.

const MAX_SECTIONS = 20;

/** Số từ tối đa không nhỏ hơn tối thiểu. */
function maxWordsRule(partName: number, sectionName: number): FormRule {
  return ({ getFieldValue }) => ({
    validator(_: unknown, value?: number | null) {
      const min: number | null | undefined = getFieldValue(['parts', partName, 'sections', sectionName, 'minWords']);
      if (value == null || min == null || value >= min) return Promise.resolve();
      return Promise.reject(new Error('Phải ≥ số từ tối thiểu'));
    }
  });
}

export function SectionsEditor({ partName }: { partName: number }) {
  return (
    <div className="mb-4">
      <p className="mb-1 text-small font-semibold text-neutral-700">Khung mục</p>
      <p className="mb-3 text-caption text-neutral-500">
        Sinh viên viết theo đúng các mục này. Mục bắt buộc phải có nội dung mới nộp được; số từ chỉ để nhắc.
      </p>
      <Form.List name={[partName, 'sections']}>
        {(items, { add, remove, move }) => (
          <div className="flex flex-col gap-2">
            {items.map((item, index) => (
              <div
                key={item.key}
                className="grid grid-cols-[1fr_auto] items-start gap-2 rounded-md border border-neutral-100 bg-neutral-25 p-2 sm:grid-cols-[1fr_96px_120px_120px_auto]"
              >
                <Form.Item
                  name={[item.name, 'title']}
                  className="mb-0"
                  rules={[
                    { required: true, whitespace: true, message: 'Nhập tên mục' },
                    { max: 200, message: 'Tối đa 200 ký tự' }
                  ]}
                >
                  <Input placeholder={`Mục ${index + 1}, vd Introduction`} />
                </Form.Item>
                <Form.Item name={[item.name, 'required']} valuePropName="checked" className="mb-0">
                  <Switch checkedChildren="Bắt buộc" unCheckedChildren="Tuỳ chọn" />
                </Form.Item>
                <Form.Item name={[item.name, 'minWords']} className="mb-0">
                  <InputNumber min={0} max={20000} className="w-full" placeholder="Từ tối thiểu" />
                </Form.Item>
                <Form.Item
                  name={[item.name, 'maxWords']}
                  className="mb-0"
                  dependencies={[['parts', partName, 'sections', item.name, 'minWords']]}
                  rules={[maxWordsRule(partName, item.name)]}
                >
                  <InputNumber min={1} max={20000} className="w-full" placeholder="Từ tối đa" />
                </Form.Item>
                <span className="flex">
                  <Tooltip title="Đưa lên">
                    <Button
                      type="text"
                      size="small"
                      icon={<ArrowUp size={14} />}
                      disabled={index === 0}
                      onClick={() => move(index, index - 1)}
                      aria-label="Đưa mục lên"
                    />
                  </Tooltip>
                  <Tooltip title="Đưa xuống">
                    <Button
                      type="text"
                      size="small"
                      icon={<ArrowDown size={14} />}
                      disabled={index === items.length - 1}
                      onClick={() => move(index, index + 1)}
                      aria-label="Đưa mục xuống"
                    />
                  </Tooltip>
                  <Tooltip title="Xoá mục">
                    <Button
                      type="text"
                      size="small"
                      danger
                      icon={<Trash2 size={14} />}
                      onClick={() => remove(item.name)}
                      aria-label="Xoá mục"
                    />
                  </Tooltip>
                </span>
              </div>
            ))}
            {items.length < MAX_SECTIONS && (
              <Button
                size="small"
                type="dashed"
                className="self-start"
                icon={<Plus size={14} />}
                onClick={() => add(newSection())}
              >
                Thêm mục
              </Button>
            )}
          </div>
        )}
      </Form.List>
    </div>
  );
}
