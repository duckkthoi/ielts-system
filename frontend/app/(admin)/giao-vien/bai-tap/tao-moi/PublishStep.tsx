'use client';

import { Alert, Button, Card, Descriptions, Form } from 'antd';
import type { FormInstance } from 'antd';
import { CheckCircle2, Send } from 'lucide-react';

import { TASK_TYPE_LABEL } from '@/lib/constants';

import { readinessProblems } from './convert';
import type { AssignmentForm } from './type';

// ─── Bước 4: kiểm tra trước khi phát hành + mở hộp thoại giao bài ─────────────
// Danh sách việc còn thiếu tính ở client cho nhanh; server kiểm lại khi phát hành (NOT_READY).

interface PublishStepProps {
  form: FormInstance<AssignmentForm>;
  saving: boolean;
  onPublish: () => void;
}

export function PublishStep({ form, saving, onPublish }: PublishStepProps) {
  const values = Form.useWatch([], { form, preserve: true }) as AssignmentForm | undefined;
  if (!values) return null;
  const problems = readinessProblems(values);
  const parts = values.parts ?? [];
  const paper = values.kind === 'PAPER';

  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
      <Card title="Tóm tắt bài tập">
        <Descriptions
          column={1}
          size="small"
          items={[
            { key: 'title', label: 'Tên bài', children: values.title || '—' },
            {
              key: 'open',
              label: 'Mở lúc',
              children: values.openAt ? values.openAt.format('HH:mm DD/MM/YYYY') : 'Ngay khi phát hành'
            },
            { key: 'due', label: 'Hạn nộp', children: values.dueAt ? values.dueAt.format('HH:mm DD/MM/YYYY') : '—' },
            { key: 'late', label: 'Nộp muộn', children: values.allowLate ? 'Cho phép' : 'Không cho phép' },
            ...(paper
              ? [
                  {
                    key: 'paper',
                    label: 'Paper theo nhóm',
                    children: `Trích dẫn ${values.citationStyle ?? 'IEEE'} · tối đa ${values.maxGroupSize ?? 4} người / nhóm · chốt nhóm ${
                      values.groupLockAt ? values.groupLockAt.format('HH:mm DD/MM/YYYY') : 'lúc hạn nộp'
                    }`
                  }
                ]
              : []),
            {
              key: 'parts',
              label: paper ? 'Các giai đoạn' : 'Các phần',
              children: (
                <ul className="m-0 list-none p-0">
                  {parts.map((p, i) => (
                    <li key={i}>
                      {i + 1}. {p.title || 'Chưa đặt tên'}{' '}
                      <span className="text-small text-neutral-500">
                        · {paper ? `${p.sections?.length ?? 0} mục` : TASK_TYPE_LABEL[p.taskType]} ·{' '}
                        {p.criteria?.length ?? 0} tiêu chí
                      </span>
                    </li>
                  ))}
                </ul>
              )
            }
          ]}
        />
      </Card>
      <Card title="Giao bài">
        {problems.length > 0 ? (
          <Alert
            type="warning"
            showIcon
            title="Cần hoàn thiện trước khi phát hành"
            description={
              <ul className="m-0 list-disc pl-4">
                {problems.map((p) => (
                  <li key={p}>{p}</li>
                ))}
              </ul>
            }
          />
        ) : (
          <p className="flex items-center gap-2 text-success">
            <CheckCircle2 size={18} /> Bài đã đủ điều kiện phát hành.
          </p>
        )}
        <p className="mt-3 text-small text-neutral-500">
          Bấm nút dưới để lưu bài rồi chọn lớp / sinh viên nhận bài. Có thể lưu nháp và phát hành sau từ trang chi tiết
          bài.
        </p>
        <Button
          type="primary"
          block
          className="mt-3"
          icon={<Send size={16} />}
          loading={saving}
          disabled={problems.length > 0}
          onClick={onPublish}
        >
          Lưu &amp; giao bài
        </Button>
      </Card>
    </div>
  );
}
