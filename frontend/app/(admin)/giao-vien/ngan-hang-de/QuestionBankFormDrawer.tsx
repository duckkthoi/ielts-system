'use client';

import { useEffect, useState } from 'react';

import { Alert, Button, Form, Input, InputNumber, Select } from 'antd';

import { AppDrawer } from '@/components/AppDrawer';
import { assignmentApi } from '@/lib/api/assignment';
import { questionBankApi } from '@/lib/api/questionBank';
import { getErrorMessage } from '@/lib/apiError';
import type { QuestionBankDetail, QuestionBankRequest } from '@/lib/types/questionBank';
import type { RubricTemplateDto, TaskType } from '@/lib/types/assignment';
import { copyCriteria } from '@/lib/utils/questionBank';
import { htmlToText, textToHtml } from '@/lib/utils/promptText';

import { TASK_OPTIONS } from './const';

type TemplateChoice = number | 'current';

interface QuestionBankForm {
  title: string;
  taskType: TaskType;
  promptText: string;
  imageUrl?: string;
  minWords?: number | null;
  suggestedMinutes?: number | null;
  tags: string[];
  templateId: TemplateChoice;
}

interface QuestionBankFormDrawerProps {
  open: boolean;
  itemId: number | null;
  onClose: () => void;
  onSaved: (message: string) => void;
}

const DEFAULTS: Record<TaskType, { minWords: number | null; suggestedMinutes: number | null }> = {
  WRITING_TASK1: { minWords: 150, suggestedMinutes: 20 },
  WRITING_TASK2: { minWords: 250, suggestedMinutes: 40 },
  CUSTOM: { minWords: null, suggestedMinutes: null }
};

export function QuestionBankFormDrawer({ open, itemId, onClose, onSaved }: QuestionBankFormDrawerProps) {
  const [form] = Form.useForm<QuestionBankForm>();
  const [detail, setDetail] = useState<QuestionBankDetail | null>(null);
  const [templates, setTemplates] = useState<RubricTemplateDto[]>([]);
  const [initializedKey, setInitializedKey] = useState<string | number | null>(null);
  const [templateLoading, setTemplateLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const editing = itemId !== null;
  const requestKey = itemId ?? 'new';
  const loading = initializedKey !== requestKey || templateLoading;

  const loadTemplates = async (taskType: TaskType) => {
    const loaded = await assignmentApi.rubricTemplates(taskType === 'CUSTOM' ? undefined : taskType);
    setTemplates(loaded);
    return loaded;
  };

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    const initialize = async () => {
      if (itemId !== null) {
        const [current, loaded] = await Promise.all([questionBankApi.detail(itemId), assignmentApi.rubricTemplates()]);
        if (cancelled) return;
        setError(null);
        setDetail(current);
        setTemplates(loaded);
        form.setFieldsValue({
          title: current.title,
          taskType: current.taskType,
          promptText: htmlToText(current.prompt),
          imageUrl: current.imageUrl ?? '',
          minWords: current.minWords,
          suggestedMinutes: current.suggestedMinutes,
          tags: current.tags,
          templateId: 'current'
        });
      } else {
        const taskType: TaskType = 'WRITING_TASK1';
        const loaded = await assignmentApi.rubricTemplates(taskType);
        if (cancelled) return;
        setError(null);
        setDetail(null);
        setTemplates(loaded);
        form.setFieldsValue({
          title: '',
          taskType,
          promptText: '',
          imageUrl: '',
          tags: [],
          ...DEFAULTS[taskType]
        });
        form.setFieldValue('templateId', loaded.find((template) => template.system)?.id ?? loaded[0]?.id);
      }
    };
    void initialize()
      .catch((reason: unknown) => {
        if (!cancelled) setError(getErrorMessage(reason, 'Không tải được dữ liệu đề'));
      })
      .finally(() => {
        if (!cancelled) setInitializedKey(requestKey);
      });
    return () => {
      cancelled = true;
    };
  }, [form, itemId, open, requestKey]);

  const changeTaskType = async (taskType: TaskType) => {
    form.setFieldsValue({ ...DEFAULTS[taskType], templateId: undefined });
    setTemplateLoading(true);
    try {
      const loaded = await loadTemplates(taskType);
      form.setFieldValue('templateId', loaded.find((template) => template.system)?.id ?? loaded[0]?.id);
    } catch (reason) {
      setTemplates([]);
      setError(getErrorMessage(reason, 'Không tải được rubric mẫu'));
    } finally {
      setTemplateLoading(false);
    }
  };

  const submit = async (values: QuestionBankForm) => {
    setSubmitting(true);
    setError(null);
    try {
      const selectedTemplate =
        values.templateId === 'current' ? null : templates.find((item) => item.id === values.templateId);
      if (values.templateId !== 'current' && !selectedTemplate) {
        setError('Vui lòng chọn rubric cho đề');
        return;
      }
      const body: QuestionBankRequest = {
        title: values.title.trim(),
        taskType: values.taskType,
        prompt: textToHtml(values.promptText),
        imageUrl: values.imageUrl?.trim() || null,
        minWords: values.minWords ?? null,
        suggestedMinutes: values.suggestedMinutes ?? null,
        tags: (values.tags ?? []).map((tag) => tag.trim()).filter(Boolean),
        criteria: selectedTemplate ? copyCriteria(selectedTemplate.criteria) : copyCriteria(detail?.criteria ?? [])
      };
      if (itemId === null) await questionBankApi.create(body);
      else await questionBankApi.replace(itemId, body);
      onSaved(itemId === null ? 'Đã thêm đề vào ngân hàng' : 'Đã cập nhật đề');
    } catch (reason) {
      setError(getErrorMessage(reason, itemId === null ? 'Không thể thêm đề' : 'Không thể cập nhật đề'));
    } finally {
      setSubmitting(false);
    }
  };

  const templateOptions = [
    ...(editing
      ? [{ value: 'current' as const, label: `Giữ rubric hiện tại (${detail?.criteria.length ?? 0} tiêu chí)` }]
      : []),
    ...templates.map((template) => ({
      value: template.id,
      label: `${template.name}${template.system ? ' · hệ thống' : ' · của tôi'} (${template.criteria.length} tiêu chí)`
    }))
  ];

  return (
    <AppDrawer
      open={open}
      title={editing ? 'Sửa đề trong ngân hàng' : 'Thêm đề vào ngân hàng'}
      subtitle="Đề và rubric được lưu thành bản riêng để dùng lại"
      size={720}
      loading={loading}
      onClose={onClose}
      footerActions={
        <>
          <Button onClick={onClose}>Huỷ</Button>
          <Button type="primary" loading={submitting} onClick={() => form.submit()}>
            {editing ? 'Lưu thay đổi' : 'Thêm đề'}
          </Button>
        </>
      }
    >
      {error && <Alert className="mb-4" type="error" showIcon message={error} />}
      <Form<QuestionBankForm> form={form} layout="vertical" requiredMark={false} onFinish={submit}>
        <Form.Item
          name="title"
          label="Tên đề"
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập tên đề' },
            { max: 150, message: 'Tối đa 150 ký tự' }
          ]}
        >
          <Input placeholder="Ví dụ: Task 2 · Remote work" />
        </Form.Item>

        <div className="grid gap-x-3 sm:grid-cols-2">
          <Form.Item name="taskType" label="Loại đề" rules={[{ required: true, message: 'Vui lòng chọn loại đề' }]}>
            <Select options={TASK_OPTIONS} disabled={editing} onChange={(value) => void changeTaskType(value)} />
          </Form.Item>
          <Form.Item name="templateId" label="Rubric" rules={[{ required: true, message: 'Vui lòng chọn rubric' }]}>
            <Select options={templateOptions} placeholder="Chọn rubric mẫu" />
          </Form.Item>
        </div>

        <Form.Item
          name="promptText"
          label="Nội dung đề"
          extra="Để một dòng trống giữa các đoạn. Hệ thống sẽ lọc định dạng trước khi lưu."
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập nội dung đề' },
            { max: 20000, message: 'Tối đa 20.000 ký tự' }
          ]}
        >
          <Input.TextArea rows={8} showCount maxLength={20000} />
        </Form.Item>

        <Form.Item name="imageUrl" label="Đường dẫn ảnh đề" rules={[{ max: 500, message: 'Tối đa 500 ký tự' }]}>
          <Input placeholder="/uploads/... hoặc https://..." />
        </Form.Item>

        <div className="grid gap-x-3 sm:grid-cols-2">
          <Form.Item name="minWords" label="Số từ tối thiểu">
            <InputNumber min={0} max={2000} className="w-full" />
          </Form.Item>
          <Form.Item name="suggestedMinutes" label="Thời gian gợi ý (phút)">
            <InputNumber min={1} max={300} className="w-full" />
          </Form.Item>
        </div>

        <Form.Item name="tags" label="Tag" extra="Gõ tag rồi nhấn Enter; tối đa 20 tag.">
          <Select mode="tags" maxCount={20} tokenSeparators={[',']} placeholder="opinion, environment, chart..." />
        </Form.Item>
      </Form>
    </AppDrawer>
  );
}
