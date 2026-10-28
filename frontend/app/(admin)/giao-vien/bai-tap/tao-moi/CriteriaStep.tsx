'use client';

import { useState } from 'react';

import { App, Button, Card, Dropdown, Empty, Form } from 'antd';
import type { FormInstance } from 'antd';
import { ChevronDown, Download, Plus } from 'lucide-react';

import { assignmentApi } from '@/lib/api/assignment';
import { TASK_TYPE_LABEL } from '@/lib/constants';
import type { RubricTemplateDto } from '@/lib/types/assignment';

import { MAX_CRITERIA, newCriterion } from './const';
import { criterionToForm } from './convert';
import { CriterionEditor } from './CriterionEditor';
import { newPaperCriterion, paperCriteria } from './paperTemplates';
import type { AssignmentForm, PartForm } from './type';

// ─── Bước 3: tiêu chí chấm + checklist cho từng phần ──────────────────────────
// Checklist "Đạt" = đặc điểm cần có; "Giới hạn điểm" = đặc điểm tiêu cực, có band tối đa (BR-16, docs/08).

function PartCriteria({
  form,
  partIndex,
  part,
  paper,
  onChanged
}: {
  form: FormInstance<AssignmentForm>;
  partIndex: number;
  part: PartForm;
  paper: boolean;
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

  const applyTemplate = (t: RubricTemplateDto) => replaceCriteria(t.name, t.criteria.map(criterionToForm));

  /** Thay toàn bộ tiêu chí của phần (hỏi lại nếu đang có tiêu chí). */
  const replaceCriteria = (name: string, criteria: PartForm['criteria']) => {
    const apply = () => {
      form.setFieldValue(['parts', partIndex, 'criteria'], criteria);
      onChanged();
    };
    if (!part.criteria?.length) {
      apply();
      return;
    }
    modal.confirm({
      title: `Thay bằng rubric "${name}"?`,
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
        paper ? (
          <Button icon={<Download size={16} />} onClick={() => replaceCriteria('Paper IT', paperCriteria())}>
            Nạp rubric mẫu paper IT
          </Button>
        ) : (
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
        )
      }
    >
      <p className="-mt-1 mb-3 text-small text-neutral-500">
        {paper ? 'Thang 10 · điểm cả giai đoạn = trung bình có trọng số các tiêu chí' : TASK_TYPE_LABEL[part.taskType]}
      </p>
      <Form.List name={['parts', partIndex, 'criteria']}>
        {(fields, { add, remove }) => (
          <div className="flex flex-col gap-3">
            {fields.length === 0 && (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={`Chưa có tiêu chí. Nạp rubric mẫu ${paper ? 'paper IT' : 'IELTS'} hoặc tự thêm tiêu chí.`}
              />
            )}
            {fields.map((field, index) => (
              <CriterionEditor
                key={field.key}
                partIndex={partIndex}
                index={index}
                paper={paper}
                onRemove={() => remove(index)}
              />
            ))}
            {fields.length < MAX_CRITERIA && (
              <Button
                type="dashed"
                icon={<Plus size={16} />}
                onClick={() => add(paper ? newPaperCriterion() : newCriterion())}
              >
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
  const paper = Form.useWatch('kind', { form, preserve: true }) === 'PAPER';
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
        <PartCriteria key={index} form={form} partIndex={index} part={part} paper={paper} onChanged={onChanged} />
      ))}
    </div>
  );
}
