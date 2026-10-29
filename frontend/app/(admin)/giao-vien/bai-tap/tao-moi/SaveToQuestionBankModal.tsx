'use client';

import { useEffect, useState } from 'react';

import { Alert, Form, Input, Modal, Select } from 'antd';

import { questionBankApi } from '@/lib/api/questionBank';
import { getErrorMessage } from '@/lib/apiError';
import { partToQuestionBank } from '@/lib/utils/questionBank';

import type { PartForm } from './type';

interface SaveToQuestionBankModalProps {
  open: boolean;
  part: PartForm | null;
  onClose: () => void;
  onSaved: () => void;
}

interface SaveForm {
  title: string;
  tags: string[];
}

/** Nhập tên/tag rồi tạo một bản sao độc lập của phần đang soạn trong ngân hàng đề. */
export function SaveToQuestionBankModal({ open, part, onClose, onSaved }: SaveToQuestionBankModalProps) {
  const [form] = Form.useForm<SaveForm>();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !part) return;
    form.setFieldsValue({ title: part.title, tags: [] });
  }, [form, open, part]);

  const close = () => {
    setError(null);
    form.resetFields();
    onClose();
  };

  const save = async () => {
    if (!part) return;
    let values: SaveForm;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      // Ngân hàng đề chỉ dành cho bài luận IELTS — không mang mức đạt của bài Paper.
      const essayPart = { ...part, criteria: part.criteria.map((c) => ({ ...c, levels: undefined })) };
      await questionBankApi.create(partToQuestionBank(essayPart, values.title, values.tags ?? []));
      onSaved();
      close();
    } catch (cause) {
      setError(getErrorMessage(cause, 'Không lưu được đề vào ngân hàng'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      title="Lưu vào ngân hàng đề"
      okText="Lưu đề"
      cancelText="Huỷ"
      confirmLoading={saving}
      destroyOnHidden
      onOk={() => void save()}
      onCancel={close}
    >
      {error && <Alert className="mb-4" type="error" showIcon title={error} />}
      {part && part.criteria.length === 0 && (
        <Alert
          className="mb-4"
          type="warning"
          showIcon
          title="Phần này chưa có tiêu chí chấm"
          description="Bạn vẫn có thể lưu đề và bổ sung rubric trong ngân hàng sau."
        />
      )}
      <Form form={form} layout="vertical" requiredMark={false}>
        <Form.Item
          name="title"
          label="Tên đề trong ngân hàng"
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập tên đề' },
            { max: 150, message: 'Tối đa 150 ký tự' }
          ]}
        >
          <Input placeholder="Ví dụ: Opinion essay – Education" />
        </Form.Item>
        <Form.Item
          name="tags"
          label="Tag"
          extra="Gõ tag rồi nhấn Enter; tối đa 20 tag."
          rules={[
            {
              validator: (_, tags: string[] = []) =>
                tags.some((tag) => tag.trim().length > 50)
                  ? Promise.reject(new Error('Mỗi tag tối đa 50 ký tự'))
                  : Promise.resolve()
            }
          ]}
        >
          <Select
            mode="tags"
            maxCount={20}
            tokenSeparators={[',']}
            placeholder="opinion, education, academic…"
            options={[]}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
