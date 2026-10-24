'use client';

import { useCallback, useState } from 'react';

import { Button, Drawer, Form, Input } from 'antd';
import type { FormRule } from 'antd';

import { classroomApi } from '@/lib/api/classroom';
import { ApiError } from '@/lib/apiError';
import type { ClassroomFormValues, ClassroomSummary } from '@/lib/types/classroom';

// ─── Drawer tạo / sửa lớp (danh sách lớp + chi tiết lớp) ──────────────────────
// Quy tắc mã lớp giống server (docs/05 A.4): 2–30 ký tự, chữ không dấu, số và . _ -; server lưu viết hoa.

const CODE_RULES: FormRule[] = [
  { required: true, whitespace: true, message: 'Vui lòng nhập mã lớp' },
  { min: 2, max: 30, message: 'Mã lớp từ 2 đến 30 ký tự' },
  { pattern: /^[A-Za-z0-9._-]+$/, message: 'Mã lớp chỉ gồm chữ không dấu, số và . _ -' }
];
const NAME_RULES: FormRule[] = [
  { required: true, whitespace: true, message: 'Vui lòng nhập tên lớp' },
  { max: 150, message: 'Tên lớp tối đa 150 ký tự' }
];

interface ClassFormDrawerProps {
  open: boolean;
  /** Có giá trị = sửa; không có = tạo mới. */
  editing: ClassroomSummary | null;
  onClose: () => void;
  onSaved: (saved: ClassroomSummary, created: boolean) => void;
}

export function ClassFormDrawer({ open, editing, onClose, onSaved }: ClassFormDrawerProps) {
  const [form] = Form.useForm<ClassroomFormValues>();
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = useCallback(
    async (values: ClassroomFormValues) => {
      setSubmitting(true);
      setFormError(null);
      const body = {
        code: values.code.trim(),
        name: values.name.trim(),
        description: values.description?.trim() || undefined
      };
      try {
        const saved = editing ? await classroomApi.updateClass(editing.id, body) : await classroomApi.createClass(body);
        onSaved(saved, !editing);
      } catch (error) {
        if (error instanceof ApiError && error.fieldErrors) {
          form.setFields(
            Object.entries(error.fieldErrors).map(([name, message]) => ({
              name: name as keyof ClassroomFormValues,
              errors: [message]
            }))
          );
        } else if (error instanceof ApiError && error.code === 'DUPLICATE') {
          form.setFields([{ name: 'code', errors: [error.message] }]);
        } else {
          setFormError(error instanceof Error ? error.message : 'Lưu thất bại');
        }
      } finally {
        setSubmitting(false);
      }
    },
    [editing, form, onSaved]
  );

  const handleAfterOpenChange = useCallback(
    (visible: boolean) => {
      if (!visible) return;
      setFormError(null);
      form.resetFields();
      if (editing) {
        form.setFieldsValue({ code: editing.code, name: editing.name, description: editing.description ?? '' });
      }
    },
    [editing, form]
  );

  return (
    <Drawer
      title={editing ? 'Sửa lớp' : 'Tạo lớp'}
      size={520}
      open={open}
      onClose={onClose}
      afterOpenChange={handleAfterOpenChange}
      destroyOnHidden
      footer={
        <div className="flex justify-end gap-2">
          <Button onClick={onClose}>Huỷ</Button>
          <Button type="primary" loading={submitting} onClick={() => form.submit()}>
            {editing ? 'Lưu' : 'Tạo lớp'}
          </Button>
        </div>
      }
    >
      {formError && <p className="mb-4 rounded-md bg-error-bg px-3 py-2 text-error">{formError}</p>}
      <Form<ClassroomFormValues> form={form} layout="vertical" requiredMark={false} onFinish={handleSubmit}>
        <Form.Item
          name="code"
          label="Mã lớp"
          rules={CODE_RULES}
          extra="Không phân biệt hoa thường, hệ thống lưu viết hoa. Dùng khi import qua admin-cli."
        >
          <Input className="font-mono" placeholder="IELTS-K15A" autoComplete="off" />
        </Form.Item>
        <Form.Item name="name" label="Tên lớp" rules={NAME_RULES}>
          <Input placeholder="IELTS K15 – Lớp A" />
        </Form.Item>
        <Form.Item name="description" label="Mô tả" rules={[{ max: 2000, message: 'Mô tả tối đa 2000 ký tự' }]}>
          <Input.TextArea rows={4} placeholder="Lịch học, mục tiêu band, ghi chú cho lớp…" />
        </Form.Item>
      </Form>
    </Drawer>
  );
}
