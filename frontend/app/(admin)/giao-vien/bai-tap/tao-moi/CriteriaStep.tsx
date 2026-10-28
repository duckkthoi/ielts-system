'use client';

import { useState } from 'react';

import { App, Button, Card, Dropdown, Empty, Form, Input, InputNumber, Segmented, Select, Tooltip } from 'antd';
import type { FormInstance, FormRule } from 'antd';
import { ChevronDown, Download, Lock, Plus, Trash2 } from 'lucide-react';

import { assignmentApi } from '@/lib/api/assignment';
import { TASK_TYPE_LABEL } from '@/lib/constants';
import type { RubricTemplateDto } from '@/lib/types/assignment';

import { CRITERION_CODE_OPTIONS, MAX_CHECKLIST, MAX_CRITERIA, newCriterion } from './const';
import { criterionToForm } from './convert';
import type { AssignmentForm, PartForm } from './type';

// ─── Bước 3: tiêu chí chấm + checklist cho từng phần ──────────────────────────
// Checklist "Đạt" = đặc điểm cần có; "Giới hạn điểm" = đặc điểm tiêu cực, có band tối đa (BR-16, docs/08).

const KIND_OPTIONS = [
  { value: 'CHECK', label: 'Cần có' },
  { value: 'LIMIT', label: 'Giới hạn điểm' }
];

/** maxBand của mục Giới hạn: trong [0, maxScore] và là bội số của bước (server kiểm lại). */
function maxBandRule(partIndex: number, criterionIndex: number): FormRule {
  return ({ getFieldValue }) => ({
    validator(_: unknown, value?: number | null) {
      if (value === null || value === undefined) return Promise.reject(new Error('Nhập band tối đa'));
      const c = getFieldValue(['parts', partIndex, 'criteria', criterionIndex]);
      if (value < 0 || value > c.maxScore) return Promise.reject(new Error(`Từ 0 tới ${c.maxScore}`));
      if (Math.abs(value / c.step - Math.round(value / c.step)) > 1e-9) {
        return Promise.reject(new Error(`Phải là bội số của ${c.step}`));
      }
      return Promise.resolve();
    }
  });
}

function CriterionEditor({ partIndex, index, onRemove }: { partIndex: number; index: number; onRemove: () => void }) {
  const base = ['parts', partIndex, 'criteria', index];
  return (
    <div className="rounded-lg border border-neutral-100 bg-neutral-0 p-4">
      <div className="grid gap-x-3 sm:grid-cols-[200px_1fr_auto]">
        <Form.Item
          name={[index, 'code']}
          label="Mã IELTS"
          getValueProps={(v) => ({ value: v ?? '' })}
          normalize={(v) => v || null}
        >
          <Select options={CRITERION_CODE_OPTIONS} />
        </Form.Item>
        <Form.Item
          name={[index, 'name']}
          label="Tên tiêu chí"
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập tên tiêu chí' },
            { max: 150, message: 'Tối đa 150 ký tự' }
          ]}
        >
          <Input placeholder="Task Response" />
        </Form.Item>
        <Form.Item label=" ">
          <Tooltip title="Xoá tiêu chí">
            <Button danger type="text" icon={<Trash2 size={16} />} onClick={onRemove} aria-label="Xoá tiêu chí" />
          </Tooltip>
        </Form.Item>
      </div>
      <Form.Item name={[index, 'description']} label="Mô tả ngắn" rules={[{ max: 2000, message: 'Tối đa 2000 ký tự' }]}>
        <Input.TextArea rows={2} placeholder="Điều giáo viên cần xem khi chấm tiêu chí này" />
      </Form.Item>
      <div className="grid gap-x-3 sm:grid-cols-3">
        <Form.Item
          name={[index, 'maxScore']}
          label="Điểm tối đa"
          rules={[{ required: true, message: 'Nhập điểm tối đa' }]}
        >
          <InputNumber min={1} max={9} step={0.5} className="w-full" />
        </Form.Item>
        <Form.Item name={[index, 'step']} label="Bước điểm" tooltip="IELTS chấm band nguyên cho từng tiêu chí (bước 1)">
          <Segmented
            options={[
              { value: 1, label: '1 (IELTS)' },
              { value: 0.5, label: '0.5' }
            ]}
          />
        </Form.Item>
        <Form.Item name={[index, 'weight']} label="Trọng số" rules={[{ required: true, message: 'Nhập trọng số' }]}>
          <InputNumber min={0.1} max={10} step={0.5} className="w-full" />
        </Form.Item>
      </div>

      <p className="mb-2 text-small font-semibold text-neutral-700">Checklist</p>
      <Form.List name={[index, 'checklist']}>
        {(items, { add, remove }) => (
          <div className="flex flex-col gap-2">
            {items.map((item, itemIndex) => (
              <div key={item.key} className="flex flex-wrap items-start gap-2 sm:flex-nowrap">
                <Form.Item name={[item.name, 'kind']} className="mb-0">
                  <Segmented size="small" options={KIND_OPTIONS} />
                </Form.Item>
                <Form.Item
                  name={[item.name, 'content']}
                  className="mb-0 min-w-0 flex-1"
                  rules={[
                    { required: true, whitespace: true, message: 'Nhập nội dung' },
                    { max: 500, message: 'Tối đa 500 ký tự' }
                  ]}
                >
                  <Input size="small" placeholder="Có đoạn tổng quan (overview)" />
                </Form.Item>
                <Form.Item noStyle shouldUpdate>
                  {({ getFieldValue }) =>
                    getFieldValue([...base, 'checklist', itemIndex, 'kind']) === 'LIMIT' ? (
                      <Form.Item
                        name={[item.name, 'maxBand']}
                        className="mb-0 w-28"
                        rules={[maxBandRule(partIndex, index)]}
                      >
                        <InputNumber
                          size="small"
                          min={0}
                          max={9}
                          step={0.5}
                          prefix={<Lock size={12} />}
                          placeholder="Tối đa"
                        />
                      </Form.Item>
                    ) : null
                  }
                </Form.Item>
                <Button
                  size="small"
                  type="text"
                  danger
                  icon={<Trash2 size={14} />}
                  onClick={() => remove(item.name)}
                  aria-label="Xoá mục checklist"
                />
              </div>
            ))}
            {items.length < MAX_CHECKLIST && (
              <Button
                size="small"
                type="dashed"
                className="self-start"
                icon={<Plus size={14} />}
                onClick={() => add({ content: '', kind: 'CHECK', maxBand: null })}
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

function PartCriteria({
  form,
  partIndex,
  part,
  onChanged
}: {
  form: FormInstance<AssignmentForm>;
  partIndex: number;
  part: PartForm;
  onChanged: () => void;
}) {
  const { modal } = App.useApp();
  const [templates, setTemplates] = useState<RubricTemplateDto[] | null>(null);
  const [loading, setLoading] = useState(false);

  const loadTemplates = async () => {
    if (templates) return;
    setLoading(true);
    try {
      setTemplates(await assignmentApi.rubricTemplates(part.taskType === 'CUSTOM' ? undefined : part.taskType));
    } catch {
      setTemplates([]);
    } finally {
      setLoading(false);
    }
  };

  const applyTemplate = (t: RubricTemplateDto) => {
    const apply = () => {
      form.setFieldValue(['parts', partIndex, 'criteria'], t.criteria.map(criterionToForm));
      onChanged();
    };
    if (!part.criteria?.length) {
      apply();
      return;
    }
    modal.confirm({
      title: `Thay bằng rubric "${t.name}"?`,
      content: `${part.criteria.length} tiêu chí đang có của phần này sẽ bị thay thế.`,
      okText: 'Thay thế',
      cancelText: 'Huỷ',
      onOk: apply
    });
  };

  return (
    <Card
      title={`Phần ${partIndex + 1} · ${part.title || 'Chưa đặt tên'}`}
      extra={
        <Dropdown
          trigger={['click']}
          onOpenChange={(open) => open && loadTemplates()}
          menu={{
            items: loading
              ? [{ key: 'loading', label: 'Đang tải…', disabled: true }]
              : templates?.length
                ? templates.map((t) => ({
                    key: String(t.id),
                    label: `${t.name}${t.system ? '' : ' (của tôi)'} · ${t.criteria.length} tiêu chí`
                  }))
                : [{ key: 'none', label: 'Chưa có rubric mẫu cho loại này', disabled: true }],
            onClick: ({ key }) => {
              const t = templates?.find((x) => String(x.id) === key);
              if (t) applyTemplate(t);
            }
          }}
        >
          <Button icon={<Download size={16} />}>
            Nạp rubric mẫu <ChevronDown size={14} />
          </Button>
        </Dropdown>
      }
    >
      <p className="-mt-1 mb-3 text-small text-neutral-500">{TASK_TYPE_LABEL[part.taskType]}</p>
      <Form.List name={['parts', partIndex, 'criteria']}>
        {(fields, { add, remove }) => (
          <div className="flex flex-col gap-3">
            {fields.length === 0 && (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Chưa có tiêu chí. Nạp rubric mẫu IELTS hoặc tự thêm tiêu chí."
              />
            )}
            {fields.map((field, index) => (
              <CriterionEditor key={field.key} partIndex={partIndex} index={index} onRemove={() => remove(index)} />
            ))}
            {fields.length < MAX_CRITERIA && (
              <Button type="dashed" icon={<Plus size={16} />} onClick={() => add(newCriterion())}>
                Thêm tiêu chí
              </Button>
            )}
          </div>
        )}
      </Form.List>
    </Card>
  );
}

export function CriteriaStep({ form, onChanged }: { form: FormInstance<AssignmentForm>; onChanged: () => void }) {
  const parts = (Form.useWatch('parts', { form, preserve: true }) ?? []) as PartForm[];
  if (parts.length === 0) {
    return (
      <Card>
        <Empty description="Bài chưa có phần nào. Quay lại bước “Các phần” để thêm." />
      </Card>
    );
  }
  return (
    <div className="flex flex-col gap-4">
      {parts.map((part, index) => (
        <PartCriteria key={index} form={form} partIndex={index} part={part} onChanged={onChanged} />
      ))}
    </div>
  );
}
