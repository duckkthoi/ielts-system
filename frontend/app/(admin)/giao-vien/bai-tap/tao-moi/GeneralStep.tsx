'use client';

import { Card, DatePicker, Form, Input, InputNumber, Switch } from 'antd';
import dayjs from 'dayjs';

// ─── Bước 1: thông tin chung ──────────────────────────────────────────────────
// Bài nháp chỉ cần tên; hạn nộp kiểm khi phát hành (server: openAt < dueAt, hạn ở tương lai).

const DATE_TIME = { showTime: { format: 'HH:mm' }, format: 'DD/MM/YYYY HH:mm', className: 'w-full' } as const;
const beforeToday = (d: dayjs.Dayjs) => d.isBefore(dayjs().startOf('day'));

export function GeneralStep() {
  return (
    <Card title="Thông tin chung">
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
    </Card>
  );
}
