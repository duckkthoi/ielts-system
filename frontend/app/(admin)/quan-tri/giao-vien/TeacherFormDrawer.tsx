'use client';

import { useCallback, useState } from 'react';

import { Button, Drawer, Form, Input } from 'antd';

import { adminApi } from '@/lib/api/admin';
import { ApiError } from '@/lib/apiError';
import type { CreatedTeacher, TeacherFormValues, UserSummary } from '@/lib/types/admin';

import { EMAIL_RULES, FULL_NAME_RULES, USERNAME_RULES } from './const';

// ─── Drawer thêm / sửa giáo viên ──────────────────────────────────────────────
// Lỗi theo từng trường (trùng tên đăng nhập, email sai…) hiện ngay dưới ô; lỗi khác hiện trên đầu form.

interface TeacherFormDrawerProps {
  open: boolean;
  /** Có giá trị = sửa; không có = thêm mới. */
  editing: UserSummary | null;
  onClose: () => void;
  onCreated: (created: CreatedTeacher) => void;
  onUpdated: () => void;
}

export function TeacherFormDrawer({ open, editing, onClose, onCreated, onUpdated }: TeacherFormDrawerProps) {
  const [form] = Form.useForm<TeacherFormValues>();
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = useCallback(
    async (values: TeacherFormValues) => {
      setSubmitting(true);
      setFormError(null);
      const body = { ...values, email: values.email?.trim() || undefined };
      try {
        if (editing) {
          await adminApi.updateTeacher(editing.id, { fullName: body.fullName, email: body.email });
          onUpdated();
        } else {
          onCreated(await adminApi.createTeacher(body));
        }
      } catch (error) {
        if (error instanceof ApiError && error.fieldErrors) {
          form.setFields(
            Object.entries(error.fieldErrors).map(([name, message]) => ({
              name: name as keyof TeacherFormValues,
              errors: [message]
            }))
          );
        } else if (error instanceof ApiError && error.code === 'DUPLICATE') {
          form.setFields([{ name: 'username', errors: [error.message] }]);
        } else {
          setFormError(error instanceof Error ? error.message : 'Lưu thất bại');
        }
      } finally {
        setSubmitting(false);
      }
    },
    [editing, form, onCreated, onUpdated]
  );

  const handleAfterOpenChange = useCallback(
    (visible: boolean) => {
      if (!visible) return;
      setFormError(null);
      form.resetFields();
      if (editing)
        form.setFieldsValue({ username: editing.username, fullName: editing.fullName, email: editing.email ?? '' });
    },
    [editing, form]
  );

  return (
    <Drawer
      title={editing ? 'Sửa thông tin giáo viên' : 'Thêm giáo viên'}
      size={480}
      open={open}
      onClose={onClose}
      afterOpenChange={handleAfterOpenChange}
      destroyOnHidden
      footer={
        <div className="flex justify-end gap-2">
          <Button onClick={onClose}>Huỷ</Button>
          <Button type="primary" loading={submitting} onClick={() => form.submit()}>
            {editing ? 'Lưu' : 'Tạo tài khoản'}
          </Button>
        </div>
      }
    >
      {formError && <p className="mb-4 rounded-md bg-error-bg px-3 py-2 text-error">{formError}</p>}
      <Form<TeacherFormValues> form={form} layout="vertical" requiredMark={false} onFinish={handleSubmit}>
        <Form.Item
          name="username"
          label="Tên đăng nhập"
          rules={editing ? [] : USERNAME_RULES}
          extra={editing ? 'Không đổi được tên đăng nhập.' : 'Chữ không dấu, số và . _ - (vd: gv.hanh)'}
        >
          <Input className="font-mono" disabled={!!editing} autoComplete="off" />
        </Form.Item>
        <Form.Item name="fullName" label="Họ tên" rules={FULL_NAME_RULES}>
          <Input placeholder="ThS. Nguyễn Thị Hạnh" />
        </Form.Item>
        <Form.Item name="email" label="Email" rules={EMAIL_RULES} extra="Dùng để gửi thông báo; có thể để trống.">
          <Input placeholder="hanh@truong.edu.vn" />
        </Form.Item>
        {!editing && (
          <p className="text-small text-neutral-500">
            Hệ thống tạo mật khẩu tạm và hiện cho bạn một lần; giáo viên phải đổi mật khẩu ở lần đăng nhập đầu.
          </p>
        )}
      </Form>
    </Drawer>
  );
}
