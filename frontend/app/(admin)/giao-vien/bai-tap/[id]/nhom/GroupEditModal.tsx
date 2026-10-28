'use client';

import { useEffect } from 'react';

import { Form, Input, Modal, Select } from 'antd';

import type { PaperGroup, TeacherGroups } from '@/lib/types/paper';

// ─── Tạo / sửa một nhóm: tên + thành viên (PUT thay toàn bộ) ──────────────────
// Chọn được cả người đang ở nhóm khác — lưu thì người đó được chuyển sang nhóm này (server lo, BR-27).

interface Values {
  name: string;
  studentIds: number[];
}

export function GroupEditModal({
  view,
  group,
  open,
  saving,
  onClose,
  onSave
}: {
  view: TeacherGroups;
  /** `null` = tạo nhóm mới. */
  group: PaperGroup | null;
  open: boolean;
  saving: boolean;
  onClose: () => void;
  onSave: (name: string, studentIds: number[]) => void;
}) {
  const [form] = Form.useForm<Values>();

  useEffect(() => {
    if (open) {
      form.setFieldsValue({
        name: group?.name ?? '',
        studentIds: group?.members.map((m) => m.studentId) ?? []
      });
    }
  }, [open, group, form]);

  // Người chưa có nhóm trước, sau đó người ở nhóm khác (ghi tên nhóm để biết sẽ bị chuyển).
  const options = [
    ...view.ungrouped.map((s) => ({ value: s.id, label: `${s.studentCode} · ${s.fullName}` })),
    ...view.groups.flatMap((g) =>
      g.members.map((m) => ({
        value: m.studentId,
        label:
          g.id === group?.id
            ? `${m.studentCode} · ${m.fullName}`
            : `${m.studentCode} · ${m.fullName} (đang ở ${g.name})`
      }))
    )
  ];

  return (
    <Modal
      open={open}
      title={group ? `Sửa ${group.name}` : 'Tạo nhóm'}
      okText="Lưu"
      cancelText="Huỷ"
      confirmLoading={saving}
      onCancel={onClose}
      onOk={() => form.validateFields().then((v) => onSave(v.name.trim(), v.studentIds ?? []))}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" requiredMark={false}>
        <Form.Item
          name="name"
          label="Tên nhóm"
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập tên nhóm' },
            { max: 100, message: 'Tối đa 100 ký tự' }
          ]}
        >
          <Input placeholder="Nhóm 1" />
        </Form.Item>
        <Form.Item
          name="studentIds"
          label={`Thành viên (tối đa ${view.maxGroupSize})`}
          extra="Chọn người đang ở nhóm khác thì người đó được chuyển sang nhóm này."
        >
          <Select
            mode="multiple"
            maxCount={view.maxGroupSize}
            options={options}
            optionFilterProp="label"
            placeholder="Gõ mã SV hoặc họ tên"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
