'use client';

import { useEffect } from 'react';

import { Form, Input, Modal, Select } from 'antd';

import type { CslItem } from '@/lib/types/paper';
import { formFromCsl, mergeForm, REFERENCE_TYPES, type ReferenceForm } from '@/lib/utils/citation';

// ─── Nhập tay / sửa một tài liệu tham khảo ────────────────────────────────────
// Dùng cho: thêm tay, sửa mục đã có, và xem lại kết quả tra DOI trước khi thêm. Lưu bằng `mergeForm` để các trường
// form không quản lý (biên tập viên, ISSN… từ DOI / BibTeX) không bị mất.

export function ReferenceFormModal({
  open,
  title,
  okText,
  initial,
  saving,
  onSave,
  onClose
}: {
  open: boolean;
  title: string;
  okText: string;
  /** Bản ghi ban đầu (sửa / kết quả tra DOI); thêm tay từ đầu → `{}`. */
  initial: CslItem;
  saving: boolean;
  onSave: (csl: CslItem) => void;
  onClose: () => void;
}) {
  const [form] = Form.useForm<ReferenceForm>();

  useEffect(() => {
    if (open) form.setFieldsValue(formFromCsl({ type: 'article-journal', ...initial }));
  }, [open, initial, form]);

  return (
    <Modal
      open={open}
      title={title}
      okText={okText}
      cancelText="Huỷ"
      confirmLoading={saving}
      onOk={() => form.submit()}
      onCancel={onClose}
      destroyOnHidden
      width={640}
    >
      <Form<ReferenceForm>
        form={form}
        layout="vertical"
        requiredMark="optional"
        onFinish={(values) => onSave(mergeForm(initial, values))}
        className="mt-2"
      >
        <div className="grid grid-cols-1 gap-x-4 sm:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
          <Form.Item
            name="title"
            label="Tiêu đề"
            rules={[{ required: true, whitespace: true, message: 'Nhập tiêu đề' }]}
          >
            <Input maxLength={2000} />
          </Form.Item>
          <Form.Item name="type" label="Loại">
            <Select options={REFERENCE_TYPES} />
          </Form.Item>
        </div>
        <Form.Item
          name="authors"
          label="Tác giả"
          extra='Mỗi dòng một người, dạng "Họ, Tên" — vd "LeCun, Yann". Tên tổ chức viết nguyên một dòng không có dấu phẩy.'
        >
          <Input.TextArea autoSize={{ minRows: 2, maxRows: 6 }} />
        </Form.Item>
        <div className="grid grid-cols-2 gap-x-4 sm:grid-cols-4">
          <Form.Item name="year" label="Năm" rules={[{ pattern: /^\d{4}$/, message: '4 chữ số' }]}>
            <Input inputMode="numeric" maxLength={4} />
          </Form.Item>
          <Form.Item name="volume" label="Tập (vol.)">
            <Input />
          </Form.Item>
          <Form.Item name="issue" label="Số (no.)">
            <Input />
          </Form.Item>
          <Form.Item name="page" label="Trang">
            <Input placeholder="436-444" />
          </Form.Item>
        </div>
        <Form.Item name="containerTitle" label="Tạp chí / hội nghị / sách chứa bài">
          <Input />
        </Form.Item>
        <div className="grid grid-cols-1 gap-x-4 sm:grid-cols-2">
          <Form.Item name="publisher" label="Nhà xuất bản / đơn vị">
            <Input />
          </Form.Item>
          <Form.Item name="doi" label="DOI">
            <Input placeholder="10.1145/3368089.3409731" />
          </Form.Item>
        </div>
        <Form.Item name="url" label="URL" rules={[{ type: 'url', message: 'URL không hợp lệ' }]}>
          <Input placeholder="https://…" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
