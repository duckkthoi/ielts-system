'use client';

import { App, Card, DatePicker, Form, Input, InputNumber, Segmented, Select, Switch } from 'antd';
import type { FormInstance } from 'antd';
import dayjs from 'dayjs';

import type { AssignmentKind } from '@/lib/types/assignment';

import { newPart } from './const';
import { CITATION_OPTIONS, defaultPaperStages } from './paperTemplates';
import type { AssignmentForm } from './type';

// ─── Bước 1: thông tin chung ──────────────────────────────────────────────────
// Bài nháp chỉ cần tên; hạn nộp kiểm khi phát hành (server: openAt < dueAt, hạn ở tương lai).
// Loại bài (Mức 4): Bài luận IELTS (cá nhân) hoặc Paper theo nhóm — đổi loại thì thay các phần bằng khung mẫu.

const DATE_TIME = { showTime: { format: 'HH:mm' }, format: 'DD/MM/YYYY HH:mm', className: 'w-full' } as const;
const beforeToday = (d: dayjs.Dayjs) => d.isBefore(dayjs().startOf('day'));

const KIND_OPTIONS: { value: AssignmentKind; label: string }[] = [
  { value: 'ESSAY', label: 'Bài luận IELTS' },
  { value: 'PAPER', label: 'Paper theo nhóm' }
];

export function GeneralStep({ form, onChanged }: { form: FormInstance<AssignmentForm>; onChanged: () => void }) {
  const { modal } = App.useApp();
  const kind = (Form.useWatch('kind', { form, preserve: true }) ?? 'ESSAY') as AssignmentKind;

  const changeKind = (next: AssignmentKind) => {
    if (next === kind) return;
    modal.confirm({
      title: next === 'PAPER' ? 'Chuyển sang Paper theo nhóm?' : 'Chuyển về Bài luận IELTS?',
      content:
        next === 'PAPER'
          ? 'Các phần hiện tại sẽ được thay bằng 2 giai đoạn mẫu (Đề cương, Bản cuối) có khung mục và rubric thang 10. Bạn sửa lại được.'
          : 'Các giai đoạn hiện tại sẽ được thay bằng Task 1 + Task 2 của IELTS.',
      okText: 'Chuyển',
      cancelText: 'Huỷ',
      onOk: () => {
        form.setFieldsValue({
          kind: next,
          parts: next === 'PAPER' ? defaultPaperStages() : [newPart('WRITING_TASK1'), newPart('WRITING_TASK2')]
        });
        onChanged();
      }
    });
  };

  return (
    <Card title="Thông tin chung">
      <Form.Item name="kind" hidden>
        <Input />
      </Form.Item>
      <Form.Item label="Loại bài">
        <Segmented<AssignmentKind> value={kind} onChange={changeKind} options={KIND_OPTIONS} />
      </Form.Item>
      <Form.Item
        name="title"
        label="Tên bài tập"
        rules={[
          { required: true, whitespace: true, message: 'Vui lòng nhập tên bài tập' },
          { max: 200, message: 'Tối đa 200 ký tự' }
        ]}
      >
        <Input placeholder="Tuần 5 – Writing practice" />
      </Form.Item>
      <Form.Item name="description" label="Mô tả / lời dặn" rules={[{ max: 5000, message: 'Tối đa 5000 ký tự' }]}>
        <Input.TextArea rows={3} placeholder="Làm đủ 2 phần, chú ý paraphrase đề bài…" />
      </Form.Item>
      <div className="grid gap-x-4 sm:grid-cols-2">
        <Form.Item name="openAt" label="Thời điểm mở" extra="Để trống = mở ngay khi phát hành">
          <DatePicker {...DATE_TIME} placeholder="Mở ngay" disabledDate={beforeToday} />
        </Form.Item>
        <Form.Item
          name="dueAt"
          label="Hạn nộp"
          dependencies={['openAt']}
          extra="Bắt buộc khi phát hành"
          rules={[
            ({ getFieldValue }) => ({
              validator(_, value?: dayjs.Dayjs | null) {
                const openAt: dayjs.Dayjs | null = getFieldValue('openAt');
                if (!value || !openAt || openAt.isBefore(value)) return Promise.resolve();
                return Promise.reject(new Error('Hạn nộp phải sau thời điểm mở'));
              }
            })
          ]}
        >
          <DatePicker {...DATE_TIME} placeholder="Chọn hạn nộp" disabledDate={beforeToday} />
        </Form.Item>
      </div>
      <div className="grid gap-x-4 sm:grid-cols-3">
        <Form.Item name="allowLate" label="Cho phép nộp muộn" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="maxRevisions" label="Số lần yêu cầu sửa tối đa" extra="0–10 lần mỗi phần">
          <InputNumber min={0} max={10} />
        </Form.Item>
        <Form.Item
          name="showSuggestionsOnRevision"
          label="Hiện gợi ý sửa khi yêu cầu sửa"
          valuePropName="checked"
          extra="Tắt để sinh viên tự tìm cách sửa (BR-21)"
        >
          <Switch />
        </Form.Item>
      </div>
      {kind === 'PAPER' && (
        <div className="grid gap-x-4 rounded-md bg-neutral-25 px-4 pt-4 sm:grid-cols-3">
          <Form.Item name="citationStyle" label="Kiểu trích dẫn">
            <Select options={CITATION_OPTIONS} />
          </Form.Item>
          <Form.Item name="maxGroupSize" label="Số người tối đa mỗi nhóm" extra="1–10 người (BR-27)">
            <InputNumber min={1} max={10} className="w-full" />
          </Form.Item>
          <Form.Item
            name="groupLockAt"
            label="Hạn chốt nhóm"
            dependencies={['dueAt']}
            extra="Sau mốc này sinh viên không tự đổi nhóm. Để trống = tới hạn nộp"
            rules={[
              ({ getFieldValue }) => ({
                validator(_, value?: dayjs.Dayjs | null) {
                  const dueAt: dayjs.Dayjs | null = getFieldValue('dueAt');
                  if (!value || !dueAt || !value.isAfter(dueAt)) return Promise.resolve();
                  return Promise.reject(new Error('Hạn chốt nhóm phải trước hạn nộp'));
                }
              })
            ]}
          >
            <DatePicker {...DATE_TIME} placeholder="Tới hạn nộp" disabledDate={beforeToday} />
          </Form.Item>
        </div>
      )}
    </Card>
  );
}
