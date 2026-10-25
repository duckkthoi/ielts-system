'use client';

import { useState } from 'react';

import { DatePicker, Form, Input, Modal } from 'antd';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';

// ─── Yêu cầu sửa bài (UC-18, Prompt 6 C) ──────────────────────────────────────
// Hạn sửa phải ở tương lai (server kiểm tra lại). Mặc định 23:59 sau 3 ngày.

export function RevisionModal({
  open,
  revisionsLeft,
  onCancel,
  onSubmit
}: {
  open: boolean;
  revisionsLeft: number;
  onCancel: () => void;
  onSubmit: (note: string | null, dueAt: string) => Promise<void>;
}) {
  const [form] = Form.useForm<{ note?: string; dueAt: Dayjs }>();
  const [submitting, setSubmitting] = useState(false);

  const handleOk = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      await onSubmit(values.note?.trim() || null, values.dueAt.toISOString());
      form.resetFields();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      title="Yêu cầu sửa bài"
      okText="Gửi yêu cầu sửa"
      cancelText="Huỷ"
      confirmLoading={submitting}
      onOk={handleOk}
      onCancel={onCancel}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{ dueAt: dayjs().add(3, 'day').hour(23).minute(59).second(0) }}
      >
        <Form.Item name="note" label="Lời nhắn cho sinh viên">
          <Input.TextArea
            autoSize={{ minRows: 3, maxRows: 6 }}
            maxLength={2000}
            placeholder="Ví dụ: Phát triển thêm ví dụ ở đoạn 2 và sửa lỗi ngữ pháp đã đánh dấu."
          />
        </Form.Item>
        <Form.Item
          name="dueAt"
          label="Hạn sửa"
          rules={[
            { required: true, message: 'Vui lòng chọn hạn sửa' },
            {
              validator: (_, v?: Dayjs) =>
                !v || v.isAfter(dayjs())
                  ? Promise.resolve()
                  : Promise.reject(new Error('Hạn sửa phải sau thời điểm hiện tại'))
            }
          ]}
        >
          <DatePicker showTime={{ format: 'HH:mm' }} format="DD/MM/YYYY HH:mm" className="w-full" />
        </Form.Item>
      </Form>
      <p className="text-small text-neutral-500">
        Sau lần này còn {Math.max(0, revisionsLeft - 1)} lần yêu cầu sửa cho bài này.
      </p>
    </Modal>
  );
}
