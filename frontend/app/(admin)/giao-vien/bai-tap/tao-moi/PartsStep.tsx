'use client';

import { Button, Card, DatePicker, Form, Input, InputNumber, Select, Tooltip } from 'antd';
import type { FormInstance } from 'antd';
import { ArrowDown, ArrowUp, Plus, Trash2 } from 'lucide-react';

import type { TaskType } from '@/lib/types/assignment';

import { MAX_PARTS, newPart, partDefaults, TASK_TYPE_OPTIONS } from './const';
import { PromptImageInput } from './PromptImageInput';
import type { AssignmentForm } from './type';

// ─── Bước 2: các phần của bài (Task 1, Task 2…) ───────────────────────────────

export function PartsStep({ form }: { form: FormInstance<AssignmentForm> }) {
  /** Đổi loại → điền lại tiêu đề / số từ / thời gian nếu đang là giá trị mặc định của loại cũ. */
  const handleTypeChange = (index: number, next: TaskType, prev: TaskType) => {
    const before = partDefaults(prev);
    const after = partDefaults(next);
    const part = form.getFieldValue(['parts', index]);
    form.setFieldValue(['parts', index], {
      ...part,
      title: !part.title || part.title === before.title ? after.title : part.title,
      minWords: part.minWords === before.minWords ? after.minWords : part.minWords,
      suggestedMinutes:
        part.suggestedMinutes === before.suggestedMinutes ? after.suggestedMinutes : part.suggestedMinutes
    });
  };

  return (
    <Form.List name="parts">
      {(fields, { add, remove, move }) => (
        <div className="flex flex-col gap-4">
          {fields.map((field, index) => (
            <Card
              key={field.key}
              title={`Phần ${index + 1}`}
              extra={
                <span className="flex gap-1">
                  <Tooltip title="Đưa lên">
                    <Button
                      type="text"
                      icon={<ArrowUp size={16} />}
                      disabled={index === 0}
                      onClick={() => move(index, index - 1)}
                      aria-label="Đưa phần lên"
                    />
                  </Tooltip>
                  <Tooltip title="Đưa xuống">
                    <Button
                      type="text"
                      icon={<ArrowDown size={16} />}
                      disabled={index === fields.length - 1}
                      onClick={() => move(index, index + 1)}
                      aria-label="Đưa phần xuống"
                    />
                  </Tooltip>
                  <Tooltip title="Xoá phần">
                    <Button
                      type="text"
                      danger
                      icon={<Trash2 size={16} />}
                      onClick={() => remove(index)}
                      aria-label="Xoá phần"
                    />
                  </Tooltip>
                </span>
              }
            >
              <div className="grid gap-x-4 sm:grid-cols-[1fr_240px]">
                <Form.Item
                  name={[field.name, 'title']}
                  label="Tiêu đề phần"
                  rules={[
                    { required: true, whitespace: true, message: 'Vui lòng nhập tiêu đề phần' },
                    { max: 200, message: 'Tối đa 200 ký tự' }
                  ]}
                >
                  <Input placeholder="Task 1 – Line graph" />
                </Form.Item>
                <Form.Item name={[field.name, 'taskType']} label="Loại">
                  <Select
                    options={TASK_TYPE_OPTIONS}
                    onChange={(next: TaskType) =>
                      handleTypeChange(index, next, form.getFieldValue(['parts', index, 'taskType']))
                    }
                  />
                </Form.Item>
              </div>
              <Form.Item
                name={[field.name, 'promptText']}
                label="Đề bài"
                extra="Để một dòng trống giữa các đoạn."
                rules={[{ max: 20000, message: 'Đề bài quá dài' }]}
              >
                <Input.TextArea
                  rows={5}
                  className="font-serif"
                  placeholder="The graph below shows … Summarise the information by selecting and reporting the main features…"
                />
              </Form.Item>
              <Form.Item name={[field.name, 'imageUrl']} label="Ảnh đề (biểu đồ, bảng…)">
                <PromptImageInput />
              </Form.Item>
              <div className="grid gap-x-4 sm:grid-cols-4">
                <Form.Item name={[field.name, 'minWords']} label="Số từ tối thiểu">
                  <InputNumber min={0} max={2000} className="w-full" />
                </Form.Item>
                <Form.Item name={[field.name, 'suggestedMinutes']} label="Thời gian gợi ý (phút)">
                  <InputNumber min={1} max={300} className="w-full" />
                </Form.Item>
                <Form.Item
                  name={[field.name, 'weight']}
                  label="Trọng số"
                  tooltip="Khi gộp band cả bài. Để trống: Task 2 = 2, còn lại = 1 (BR-12)."
                >
                  <InputNumber min={0.1} max={10} step={0.5} className="w-full" placeholder="Mặc định" />
                </Form.Item>
                <Form.Item name={[field.name, 'partDueAt']} label="Hạn riêng" tooltip="Để trống = dùng hạn nộp chung">
                  <DatePicker showTime={{ format: 'HH:mm' }} format="DD/MM/YYYY HH:mm" className="w-full" />
                </Form.Item>
              </div>
            </Card>
          ))}
          {fields.length < MAX_PARTS && (
            <div className="grid gap-2 sm:grid-cols-3">
              {(['WRITING_TASK1', 'WRITING_TASK2', 'CUSTOM'] as TaskType[]).map((type) => (
                <Button key={type} type="dashed" icon={<Plus size={16} />} onClick={() => add(newPart(type))}>
                  Thêm {type === 'CUSTOM' ? 'phần tự do' : type === 'WRITING_TASK1' ? 'Task 1' : 'Task 2'}
                </Button>
              ))}
            </div>
          )}
        </div>
      )}
    </Form.List>
  );
}
