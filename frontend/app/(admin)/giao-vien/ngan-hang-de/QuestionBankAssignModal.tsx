'use client';

import { useEffect, useState } from 'react';

import { Alert, App, DatePicker, Form, Input, InputNumber, Modal, Skeleton, Switch } from 'antd';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { useRouter } from 'next/navigation';

import { assignmentPath } from '@/components/AssignmentActions';
import { AssignTargetsModal } from '@/components/AssignTargetsModal';
import { assignmentApi } from '@/lib/api/assignment';
import { questionBankApi } from '@/lib/api/questionBank';
import { getErrorMessage } from '@/lib/apiError';
import type { AssignmentDetail } from '@/lib/types/assignment';
import type { QuestionBankDetail } from '@/lib/types/questionBank';
import { questionBankToAssignment } from '@/lib/utils/questionBank';

interface QuickAssignForm {
  title: string;
  openAt: Dayjs | null;
  dueAt: Dayjs;
  allowLate: boolean;
  maxRevisions: number;
  showSuggestionsOnRevision: boolean;
}

interface QuestionBankAssignModalProps {
  itemId: number | null;
  onClose: () => void;
}

const DATE_TIME = { showTime: { format: 'HH:mm' }, format: 'DD/MM/YYYY HH:mm', className: 'w-full' } as const;
const beforeToday = (value: Dayjs) => value.isBefore(dayjs().startOf('day'));

export function QuestionBankAssignModal({ itemId, onClose }: QuestionBankAssignModalProps) {
  const router = useRouter();
  const { message } = App.useApp();
  const [form] = Form.useForm<QuickAssignForm>();
  const [result, setResult] = useState<{ itemId: number; detail: QuestionBankDetail | null } | null>(null);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [draft, setDraft] = useState<AssignmentDetail | null>(null);
  const detail = result?.itemId === itemId ? result.detail : null;
  const loading = itemId !== null && result?.itemId !== itemId;

  useEffect(() => {
    if (itemId === null) return;
    let cancelled = false;
    void questionBankApi
      .detail(itemId)
      .then((item) => {
        if (cancelled) return;
        setResult({ itemId, detail: item });
        form.setFieldsValue({ title: item.title });
      })
      .catch(() => {
        if (!cancelled) setResult({ itemId, detail: null });
      });
    return () => {
      cancelled = true;
    };
  }, [itemId, form]);

  const createDraft = async () => {
    if (!detail) return;
    setCreating(true);
    setCreateError(null);
    try {
      const values = await form.validateFields();
      const created = await assignmentApi.create(
        questionBankToAssignment(detail, {
          title: values.title,
          openAt: values.openAt?.toISOString() ?? null,
          dueAt: values.dueAt.toISOString(),
          allowLate: values.allowLate,
          maxRevisions: values.maxRevisions,
          showSuggestionsOnRevision: values.showSuggestionsOnRevision
        })
      );
      setDraft(created);
      message.success('Đã tạo bài nháp · chọn người nhận để phát hành');
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      setCreateError(getErrorMessage(error, 'Chưa tạo được bài tập'));
    } finally {
      setCreating(false);
    }
  };

  const closeTargets = () => {
    if (draft) message.info('Bài đã được giữ trong danh sách dưới dạng nháp');
    setDraft(null);
    onClose();
  };

  return (
    <>
      <Modal
        title="Giao đề trực tiếp"
        open={itemId !== null && draft === null}
        width={620}
        okText="Tiếp tục chọn người nhận"
        cancelText="Huỷ"
        okButtonProps={{ loading: creating, disabled: loading || !detail || detail.criteria.length === 0 }}
        onOk={createDraft}
        onCancel={onClose}
        destroyOnHidden
      >
        {loading ? (
          <Skeleton active paragraph={{ rows: 5 }} />
        ) : !detail ? (
          <Alert
            type="error"
            showIcon
            title="Không tải được đề"
            description="Đề không tồn tại hoặc không thuộc ngân hàng của bạn."
          />
        ) : (
          <Form<QuickAssignForm>
            form={form}
            layout="vertical"
            requiredMark={false}
            initialValues={{
              title: detail.title,
              openAt: null,
              allowLate: true,
              maxRevisions: 2,
              showSuggestionsOnRevision: false
            }}
          >
            <Alert
              className="mb-4"
              type="info"
              showIcon
              title={`Bài sẽ có 1 phần: ${detail.title}`}
              description="Đề, ảnh và rubric được sao chép từ ngân hàng. Sau khi phát hành, nội dung này không thể sửa."
            />
            {detail.criteria.length === 0 && (
              <Alert
                className="mb-4"
                type="warning"
                showIcon
                title="Đề chưa có rubric"
                description="Hãy sửa đề và thêm ít nhất một tiêu chí chấm trước khi giao."
              />
            )}
            {createError && (
              <Alert className="mb-4" type="error" showIcon title="Chưa tạo được bài" description={createError} />
            )}
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
            <div className="grid gap-x-4 sm:grid-cols-2">
              <Form.Item name="openAt" label="Thời điểm mở" extra="Để trống = mở ngay khi phát hành">
                <DatePicker {...DATE_TIME} placeholder="Mở ngay" disabledDate={beforeToday} />
              </Form.Item>
              <Form.Item
                name="dueAt"
                label="Hạn nộp"
                dependencies={['openAt']}
                rules={[
                  { required: true, message: 'Vui lòng chọn hạn nộp' },
                  ({ getFieldValue }) => ({
                    validator(_, value?: Dayjs) {
                      if (!value || value.isBefore(dayjs()))
                        return Promise.reject(new Error('Hạn nộp phải ở tương lai'));
                      const openAt = getFieldValue('openAt') as Dayjs | null;
                      if (!openAt || openAt.isBefore(value)) return Promise.resolve();
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
              <Form.Item name="maxRevisions" label="Số lần yêu cầu sửa">
                <InputNumber min={0} max={10} />
              </Form.Item>
              <Form.Item name="showSuggestionsOnRevision" label="Hiện gợi ý sửa" valuePropName="checked">
                <Switch />
              </Form.Item>
            </div>
          </Form>
        )}
      </Modal>

      {draft && (
        <AssignTargetsModal
          assignmentId={draft.id}
          mode="publish"
          open
          dueAt={draft.dueAt}
          onClose={closeTargets}
          onDone={() => {
            const id = draft.id;
            setDraft(null);
            onClose();
            router.push(assignmentPath(id));
          }}
        />
      )}
    </>
  );
}
