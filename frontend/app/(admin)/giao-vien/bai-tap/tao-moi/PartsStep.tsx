'use client';

import { useState } from 'react';

import { App, Button, Card, Form, Input, Select, Tooltip } from 'antd';
import type { FormInstance } from 'antd';
import { ArrowDown, ArrowUp, BookOpen, BookmarkPlus, Plus, Trash2 } from 'lucide-react';

import type { QuestionBankDetail } from '@/lib/types/questionBank';
import type { TaskType } from '@/lib/types/assignment';
import { questionBankToPart } from '@/lib/utils/questionBank';

import { MAX_PARTS, newPart, partDefaults, TASK_TYPE_OPTIONS } from './const';
import { newPaperStage, newSection } from './paperTemplates';
import { PartTimingFields } from './PartTimingFields';
import { PromptImageInput } from './PromptImageInput';
import { QuestionBankPickerModal } from './QuestionBankPickerModal';
import { SaveToQuestionBankModal } from './SaveToQuestionBankModal';
import { SectionsEditor } from './SectionsEditor';
import type { AssignmentForm, PartForm } from './type';

// ─── Bước 2: các phần của bài (Task 1, Task 2…) — bài Paper: các giai đoạn + khung mục ─

export function PartsStep({ form, onChanged }: { form: FormInstance<AssignmentForm>; onChanged: () => void }) {
  const { message, modal } = App.useApp();
  const [pickerIndex, setPickerIndex] = useState<number | null>(null);
  const [savingPart, setSavingPart] = useState<PartForm | null>(null);
  const paper = Form.useWatch('kind', { form, preserve: true }) === 'PAPER';

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

  const openSave = (index: number) => {
    const part = form.getFieldValue(['parts', index]) as PartForm;
    if (!part.promptText?.trim()) {
      message.warning('Hãy nhập đề bài trước khi lưu vào ngân hàng');
      return;
    }
    setSavingPart(part);
  };

  const applyBankItem = (index: number, item: QuestionBankDetail) => {
    const current = form.getFieldValue(['parts', index]) as PartForm;
    const apply = () => {
      form.setFieldValue(['parts', index], {
        ...current,
        ...questionBankToPart(item),
        // Ngân hàng không mang cài đặt chỉ thuộc lần giao bài hiện tại.
        partDueAt: current.partDueAt,
        weight: current.weight
      });
      onChanged();
      message.success(`Đã nạp đề “${item.title}”`);
    };
    if (!current.promptText?.trim() && !current.imageUrl && !current.criteria?.length) {
      apply();
      return;
    }
    modal.confirm({
      title: `Thay nội dung phần bằng “${item.title}”?`,
      content:
        'Đề bài, ảnh, số từ, thời gian gợi ý và rubric hiện tại sẽ bị thay thế. Hạn riêng và trọng số được giữ nguyên.',
      okText: 'Thay thế',
      cancelText: 'Huỷ',
      onOk: apply
    });
  };

  return (
    <>
      <Form.List name="parts">
        {(fields, { add, remove, move }) => (
          <div className="flex flex-col gap-4">
            {fields.map((field, index) => (
              <Card
                key={field.key}
                title={paper ? `Giai đoạn ${index + 1}` : `Phần ${index + 1}`}
                extra={
                  <span className="flex flex-wrap justify-end gap-1">
                    {!paper && (
                      <>
                        <Tooltip title="Sao chép một đề và rubric đã lưu vào phần này">
                          <Button type="text" icon={<BookOpen size={16} />} onClick={() => setPickerIndex(index)}>
                            <span className="hidden lg:inline">Chọn từ ngân hàng</span>
                          </Button>
                        </Tooltip>
                        <Tooltip title="Lưu đề và rubric của phần này để dùng lại">
                          <Button type="text" icon={<BookmarkPlus size={16} />} onClick={() => openSave(index)}>
                            <span className="hidden lg:inline">Lưu vào ngân hàng</span>
                          </Button>
                        </Tooltip>
                      </>
                    )}
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
                <div className={paper ? 'grid gap-x-4' : 'grid gap-x-4 sm:grid-cols-[1fr_240px]'}>
                  <Form.Item
                    name={[field.name, 'title']}
                    label={paper ? 'Tên giai đoạn' : 'Tiêu đề phần'}
                    rules={[
                      { required: true, whitespace: true, message: 'Vui lòng nhập tiêu đề phần' },
                      { max: 200, message: 'Tối đa 200 ký tự' }
                    ]}
                  >
                    <Input placeholder={paper ? 'Đề cương' : 'Task 1 – Line graph'} />
                  </Form.Item>
                  <Form.Item name={[field.name, 'taskType']} label="Loại" hidden={paper}>
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
                  label={paper ? 'Yêu cầu của giai đoạn' : 'Đề bài'}
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
                {paper && <SectionsEditor partName={field.name} />}
                <PartTimingFields form={form} fieldName={field.name} paper={paper} />
              </Card>
            ))}
            {fields.length < MAX_PARTS && paper && (
              <Button
                type="dashed"
                icon={<Plus size={16} />}
                onClick={() => add(newPaperStage(`Giai đoạn ${fields.length + 1}`, [newSection()], ''))}
              >
                Thêm giai đoạn
              </Button>
            )}
            {fields.length < MAX_PARTS && !paper && (
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
      {pickerIndex !== null && (
        <QuestionBankPickerModal
          open
          taskType={(form.getFieldValue(['parts', pickerIndex]) as PartForm | undefined)?.taskType}
          onClose={() => setPickerIndex(null)}
          onPick={(item) => applyBankItem(pickerIndex, item)}
        />
      )}
      <SaveToQuestionBankModal
        open={savingPart !== null}
        part={savingPart}
        onClose={() => setSavingPart(null)}
        onSaved={() => message.success('Đã lưu đề vào ngân hàng')}
      />
    </>
  );
}
