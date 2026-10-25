'use client';

import { useCallback, useState } from 'react';

import { Button, DatePicker, Drawer, Form, Input, InputNumber, Switch } from 'antd';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';

import { assignmentApi } from '@/lib/api/assignment';
import { getErrorMessage } from '@/lib/apiError';
import type { AssignmentDetail } from '@/lib/types/assignment';
import { parseServerDate } from '@/lib/utils/date';

// ─── Sửa cài đặt bài (mọi trạng thái, BR-06): tên, mô tả, hạn nộp, hạn riêng từng phần, tuỳ chọn ─
// Không sửa đề / rubric ở đây (bài đã phát hành thì khoá nội dung).

interface SettingsForm {
  title: string;
  description?: string;
  dueAt?: Dayjs | null;
  allowLate: boolean;
  maxRevisions: number;
  showSuggestionsOnRevision: boolean;
  partDueAts: Record<string, Dayjs | null | undefined>;
}

const toDayjs = (iso: string | null) => (iso ? parseServerDate(iso) : null);

interface SettingsDrawerProps {
  assignment: AssignmentDetail;
  open: boolean;
  onClose: () => void;
  onSaved: (updated: AssignmentDetail) => void;
}

export function SettingsDrawer({ assignment, open, onClose, onSaved }: SettingsDrawerProps) {
  const [form] = Form.useForm<SettingsForm>();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleAfterOpenChange = useCallback(
    (visible: boolean) => {
      if (!visible) return;
      setError(null);
      form.setFieldsValue({
        title: assignment.title,
        description: assignment.description ?? '',
        dueAt: toDayjs(assignment.dueAt),
        allowLate: assignment.allowLate,
        maxRevisions: assignment.maxRevisions,
        showSuggestionsOnRevision: assignment.showSuggestionsOnRevision,
        partDueAts: Object.fromEntries(assignment.parts.map((p) => [String(p.id), toDayjs(p.partDueAt)]))
      });
    },
    [assignment, form]
  );

  const handleSubmit = useCallback(
    async (values: SettingsForm) => {
      setSubmitting(true);
      setError(null);
      try {
        const updated = await assignmentApi.updateSettings(assignment.id, {
          title: values.title.trim(),
          description: values.description?.trim() ?? '',
          dueAt: values.dueAt?.toISOString(),
          allowLate: values.allowLate,
          maxRevisions: values.maxRevisions,
          showSuggestionsOnRevision: values.showSuggestionsOnRevision,
          // Gửi đủ mọi phần: null = bỏ hạn riêng.
          partDueAts: Object.fromEntries(
            assignment.parts.map((p) => [p.id, values.partDueAts?.[String(p.id)]?.toISOString() ?? null])
          )
        });
        onSaved(updated);
      } catch (e) {
        setError(getErrorMessage(e, 'Lưu thất bại'));
      } finally {
        setSubmitting(false);
      }
    },
    [assignment, onSaved]
  );

  const published = assignment.status !== 'DRAFT';

  return (
    <Drawer
      title="Cài đặt bài tập"
      size={520}
      open={open}
      onClose={onClose}
      afterOpenChange={handleAfterOpenChange}
      footer={
        <div className="flex justify-end gap-2">
          <Button onClick={onClose}>Huỷ</Button>
          <Button type="primary" loading={submitting} onClick={() => form.submit()}>
            Lưu
          </Button>
        </div>
      }
    >
      {error && <p className="mb-4 rounded-md bg-error-bg px-3 py-2 text-error">{error}</p>}
      {published && (
        <p className="mb-4 rounded-md bg-info-bg px-3 py-2 text-small text-info">
          Bài đã phát hành: chỉ đổi được tên, mô tả, hạn nộp và các tuỳ chọn. Đề bài và rubric giữ nguyên để không làm
          sai lệch bài đã nộp.
        </p>
      )}
      <Form<SettingsForm> form={form} layout="vertical" requiredMark={false} onFinish={handleSubmit}>
        <Form.Item
          name="title"
          label="Tên bài tập"
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập tên bài tập' },
            { max: 200, message: 'Tối đa 200 ký tự' }
          ]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="description" label="Mô tả" rules={[{ max: 5000, message: 'Tối đa 5000 ký tự' }]}>
          <Input.TextArea rows={3} />
        </Form.Item>
        <Form.Item name="dueAt" label="Hạn nộp">
          <DatePicker
            showTime={{ format: 'HH:mm' }}
            format="DD/MM/YYYY HH:mm"
            className="w-full"
            disabledDate={(d) => d.isBefore(dayjs().startOf('day'))}
          />
        </Form.Item>
        {assignment.parts.length > 1 &&
          assignment.parts.map((p) => (
            <Form.Item
              key={p.id}
              name={['partDueAts', String(p.id)]}
              label={`Hạn riêng · ${p.title}`}
              extra="Để trống = dùng hạn nộp chung"
            >
              <DatePicker showTime={{ format: 'HH:mm' }} format="DD/MM/YYYY HH:mm" className="w-full" allowClear />
            </Form.Item>
          ))}
        <Form.Item name="allowLate" label="Cho phép nộp muộn" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item
          name="maxRevisions"
          label="Số lần yêu cầu sửa tối đa"
          extra="Số lần bạn được trả bài kèm yêu cầu sửa cho mỗi phần (0–10)."
        >
          <InputNumber min={0} max={10} />
        </Form.Item>
        <Form.Item
          name="showSuggestionsOnRevision"
          label="Hiện gợi ý sửa khi yêu cầu sửa"
          valuePropName="checked"
          extra="Tắt để sinh viên tự tìm cách sửa lỗi (BR-21)."
        >
          <Switch />
        </Form.Item>
      </Form>
    </Drawer>
  );
}
