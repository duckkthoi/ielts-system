'use client';

import { useEffect, useState } from 'react';

import { Form, Input, Modal, Select } from 'antd';

import { LIVE_COMMENT_KIND_META, type LiveCommentKind } from '@/lib/theme/status';

// ─── Form nhận xét trực tiếp của giáo viên (H4) ───────────────────────────────
// Đoạn được chọn chỉ đọc; loại phản hồi + nội dung + gợi ý sửa (tuỳ chọn). Sửa thì không đổi đoạn được chọn.

export interface LiveCommentFormValues {
  kind: LiveCommentKind;
  comment: string;
  suggestion?: string;
}

const KIND_OPTIONS = (Object.keys(LIVE_COMMENT_KIND_META) as LiveCommentKind[]).map((value) => ({
  value,
  label: LIVE_COMMENT_KIND_META[value].label
}));

export function LiveCommentFormModal({
  open,
  quote,
  initial,
  editing,
  onSubmit,
  onClose
}: {
  open: boolean;
  quote: string;
  initial: LiveCommentFormValues;
  editing: boolean;
  onSubmit: (values: LiveCommentFormValues) => Promise<void>;
  onClose: () => void;
}) {
  const [form] = Form.useForm<LiveCommentFormValues>();
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) form.setFieldsValue({ suggestion: '', ...initial });
  }, [open, initial, form]);

  const submit = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      await onSubmit(values);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      title={editing ? 'Sửa nhận xét' : 'Nhận xét đoạn đã chọn'}
      okText={editing ? 'Lưu' : 'Gửi nhận xét'}
      cancelText="Huỷ"
      confirmLoading={saving}
      onOk={submit}
      onCancel={onClose}
      forceRender
    >
      <blockquote className="m-0 mb-4 max-h-32 overflow-y-auto border-l-2 border-highlight-comment-line bg-neutral-25 px-3 py-2 font-serif text-small text-neutral-700">
        {quote}
      </blockquote>
      <Form form={form} layout="vertical" requiredMark={false}>
        <Form.Item name="kind" label="Loại phản hồi" rules={[{ required: true }]}>
          <Select options={KIND_OPTIONS} />
        </Form.Item>
        <Form.Item
          name="comment"
          label="Nội dung"
          rules={[
            { required: true, whitespace: true, message: 'Nhập nội dung nhận xét' },
            { max: 2000, message: 'Tối đa 2000 ký tự' }
          ]}
        >
          <Input.TextArea autoSize={{ minRows: 3, maxRows: 8 }} placeholder="Chỉ ra vấn đề và hướng sửa" />
        </Form.Item>
        <Form.Item name="suggestion" label="Gợi ý sửa (tuỳ chọn)" rules={[{ max: 2000, message: 'Tối đa 2000 ký tự' }]}>
          <Input.TextArea autoSize={{ minRows: 2, maxRows: 6 }} placeholder="Cách viết lại đoạn này" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
