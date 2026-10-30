'use client';

import { DatePicker, Form, InputNumber, Switch } from 'antd';
import type { FormInstance } from 'antd';

import type { AssignmentForm } from './type';

// ─── Các ô của một phần: số từ, thời gian, trọng số, hạn riêng; bài luận có thêm chế độ thi ─
// Bài Paper không dùng số từ cả phần (đã có theo từng mục), thời gian gợi ý, chế độ thi.

export function PartTimingFields({
  form,
  fieldName,
  paper
}: {
  form: FormInstance<AssignmentForm>;
  fieldName: number;
  paper: boolean;
}) {
  return (
    <>
      <div className="grid gap-x-4 sm:grid-cols-4">
        {!paper && (
          <>
            <Form.Item name={[fieldName, 'minWords']} label="Số từ tối thiểu">
              <InputNumber min={0} max={2000} className="w-full" />
            </Form.Item>
            <Form.Item name={[fieldName, 'suggestedMinutes']} label="Thời gian gợi ý (phút)">
              <InputNumber min={1} max={300} className="w-full" />
            </Form.Item>
          </>
        )}
        <Form.Item
          name={[fieldName, 'weight']}
          label="Trọng số"
          tooltip={
            paper
              ? 'Khi gộp điểm các giai đoạn thành điểm cả bài. Để trống = 1.'
              : 'Khi gộp band cả bài. Để trống: Task 2 = 2, còn lại = 1 (BR-12).'
          }
        >
          <InputNumber min={0.1} max={10} step={0.5} className="w-full" placeholder="Mặc định" />
        </Form.Item>
        <Form.Item name={[fieldName, 'partDueAt']} label="Hạn riêng" tooltip="Để trống = dùng hạn nộp chung">
          <DatePicker showTime={{ format: 'HH:mm' }} format="DD/MM/YYYY HH:mm" className="w-full" />
        </Form.Item>
      </div>
      {!paper && (
        <div className="grid gap-x-4 sm:grid-cols-4">
          <Form.Item
            name={[fieldName, 'examMode']}
            label="Chế độ thi"
            valuePropName="checked"
            tooltip="Sinh viên bấm Bắt đầu thì đồng hồ chạy; hết giờ vẫn nộp được nhưng bị đánh dấu quá thời gian. Rời trang, dán văn bản được ghi lại và báo bạn."
          >
            <Switch />
          </Form.Item>
          <Form.Item noStyle dependencies={[['parts', fieldName, 'examMode']]}>
            {() =>
              form.getFieldValue(['parts', fieldName, 'examMode']) ? (
                <Form.Item
                  name={[fieldName, 'examDurationMinutes']}
                  label="Thời gian thi (phút)"
                  tooltip="Để trống = lấy thời gian gợi ý"
                >
                  <InputNumber min={1} max={300} className="w-full" placeholder="Theo thời gian gợi ý" />
                </Form.Item>
              ) : null
            }
          </Form.Item>
        </div>
      )}
    </>
  );
}
