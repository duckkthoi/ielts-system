'use client';

import { useCallback, useEffect, useState } from 'react';

import { Alert, App, Button, Card, Form, Result, Skeleton, Steps } from 'antd';
import { ArrowLeft, Save } from 'lucide-react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';

import { assignmentPath } from '@/components/AssignmentActions';
import { AssignTargetsModal } from '@/components/AssignTargetsModal';
import { assignmentApi } from '@/lib/api/assignment';
import { getErrorMessage } from '@/lib/apiError';
import type { AssignmentDetail } from '@/lib/types/assignment';

import { EMPTY_FORM, STEPS } from './const';
import { detailToForm, formToRequest } from './convert';
import { CriteriaStep } from './CriteriaStep';
import { GeneralStep } from './GeneralStep';
import { PartsStep } from './PartsStep';
import { PublishStep } from './PublishStep';
import type { AssignmentForm } from './type';

// ─── Giáo viên: trình soạn bài nhiều bước (UC-12, UC-13) ──────────────────────
// `?id=` = sửa bài nháp (PUT thay toàn bộ); không có = tạo mới (POST, xong chuyển sang `?id=`).
// Một Form chung cho 4 bước; bước chưa hiện vẫn giữ giá trị (preserve). Chỉ bài DRAFT sửa được nội dung (BR-06).

const LIST_PATH = '/giao-vien/bai-tap';

/** Trường cần kiểm khi bấm "Tiếp tục" ở từng bước (bước 2, 3 kiểm đệ quy cả mảng parts đang hiện). */
const STEP_FIELDS: (string | (string | number)[])[][] = [
  ['title', 'description', 'openAt', 'dueAt', 'maxRevisions', 'maxGroupSize', 'groupLockAt'],
  [['parts']],
  [['parts']],
  []
];

export default function AssignmentEditorPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const idParam = useSearchParams().get('id');
  const editingId = idParam ? Number(idParam) : null;
  const [form] = Form.useForm<AssignmentForm>();
  const [step, setStep] = useState(0);
  const [loaded, setLoaded] = useState<AssignmentDetail | null>(null);
  const [loadError, setLoadError] = useState<'not-found' | 'locked' | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [dirty, setDirty] = useState(false);
  const [publishId, setPublishId] = useState<number | null>(null);

  // Sửa nháp: tải bài, đổ vào form. Bài đã phát hành → không sửa nội dung ở đây.
  useEffect(() => {
    if (editingId === null) return;
    let cancelled = false;
    assignmentApi
      .detail(editingId)
      .then((detail) => {
        if (cancelled) return;
        if (detail.status !== 'DRAFT') {
          setLoadError('locked');
          return;
        }
        form.setFieldsValue(detailToForm(detail));
        setLoaded(detail);
      })
      .catch(() => {
        if (!cancelled) setLoadError('not-found');
      });
    return () => {
      cancelled = true;
    };
  }, [editingId, form]);

  // Rời trang khi còn thay đổi chưa lưu → trình duyệt hỏi lại.
  useEffect(() => {
    if (!dirty) return;
    const handler = (e: BeforeUnloadEvent) => e.preventDefault();
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [dirty]);

  /** Lưu nháp; trả id bài (tạo mới thì đổi URL sang `?id=` để lần sau là sửa). */
  const save = useCallback(async (): Promise<number | null> => {
    try {
      await form.validateFields(['title']);
    } catch {
      setStep(0);
      return null;
    }
    setSaving(true);
    setSaveError(null);
    try {
      const body = formToRequest(form.getFieldsValue(true) as AssignmentForm);
      const saved = editingId ? await assignmentApi.replaceDraft(editingId, body) : await assignmentApi.create(body);
      setLoaded(saved);
      setDirty(false);
      if (!editingId) router.replace(`/giao-vien/bai-tap/tao-moi?id=${saved.id}`);
      return saved.id;
    } catch (error) {
      setSaveError(getErrorMessage(error, 'Lưu thất bại'));
      return null;
    } finally {
      setSaving(false);
    }
  }, [form, editingId, router]);

  const handleSaveDraft = useCallback(async () => {
    if ((await save()) !== null) message.success('Đã lưu bài tập nháp');
  }, [save, message]);

  const handleNext = useCallback(async () => {
    try {
      await form.validateFields(STEP_FIELDS[step], { recursive: true });
      setStep((s) => Math.min(s + 1, STEPS.length - 1));
    } catch {
      message.warning('Hãy sửa các ô đang báo lỗi trước khi tiếp tục');
    }
  }, [form, step, message]);

  const handlePublish = useCallback(async () => {
    const id = await save();
    if (id !== null) setPublishId(id);
  }, [save]);

  const back = (
    <Link
      href={editingId ? assignmentPath(editingId) : LIST_PATH}
      className="mb-3 inline-flex items-center gap-1 text-small text-neutral-500 hover:text-brand-600"
    >
      <ArrowLeft size={14} /> {editingId ? 'Chi tiết bài' : 'Bài tập'}
    </Link>
  );

  if (loadError) {
    return (
      <div className="mx-auto w-full max-w-5xl px-4 py-6 sm:px-6">
        {back}
        <Result
          status={loadError === 'locked' ? 'warning' : '404'}
          title={loadError === 'locked' ? 'Bài đã phát hành, không sửa được nội dung' : 'Không tìm thấy bài tập'}
          subTitle={
            loadError === 'locked'
              ? 'Đề bài và rubric được giữ nguyên để không làm sai lệch bài đã nộp. Bạn vẫn đổi được tên, mô tả, hạn nộp trong “Cài đặt” của bài, hoặc nhân bản để soạn bài mới.'
              : 'Bài tập không tồn tại hoặc không phải của bạn.'
          }
          extra={
            editingId && (
              <Link href={assignmentPath(editingId)}>
                <Button type="primary">Về chi tiết bài</Button>
              </Link>
            )
          }
        />
      </div>
    );
  }

  const loading = editingId !== null && !loaded;

  return (
    <div className="mx-auto w-full max-w-5xl px-4 pt-6 sm:px-6">
      {back}
      <h1 className="mb-4 text-h1 font-semibold text-neutral-900">
        {editingId ? `Sửa bài nháp${loaded ? ` · ${loaded.title}` : ''}` : 'Tạo bài tập'}
      </h1>
      <Card className="mb-4" styles={{ body: { padding: '16px 24px' } }}>
        <Steps
          current={step}
          size="small"
          onChange={(next) => (next < step ? setStep(next) : handleNext())}
          items={STEPS.map((s) => ({ title: s.title }))}
        />
      </Card>
      {saveError && (
        <Alert
          className="mb-4"
          type="error"
          showIcon
          closable={{ onClose: () => setSaveError(null) }}
          title="Chưa lưu được"
          description={saveError}
        />
      )}
      {loading ? (
        <Card>
          <Skeleton active paragraph={{ rows: 8 }} />
        </Card>
      ) : (
        <Form<AssignmentForm>
          form={form}
          layout="vertical"
          requiredMark={false}
          initialValues={EMPTY_FORM}
          onValuesChange={() => setDirty(true)}
          scrollToFirstError
        >
          {step === 0 && <GeneralStep form={form} onChanged={() => setDirty(true)} />}
          {step === 1 && <PartsStep form={form} onChanged={() => setDirty(true)} />}
          {step === 2 && <CriteriaStep form={form} onChanged={() => setDirty(true)} />}
          {step === 3 && <PublishStep form={form} saving={saving} onPublish={handlePublish} />}
        </Form>
      )}

      <div className="sticky bottom-0 z-10 -mx-4 mt-6 border-t border-neutral-100 bg-neutral-0/95 backdrop-blur sm:-mx-6">
        <div className="flex items-center justify-between gap-2 px-4 py-3 sm:px-6">
          <Button icon={<Save size={16} />} loading={saving} onClick={handleSaveDraft} disabled={loading}>
            Lưu nháp
          </Button>
          <span className="flex gap-2">
            <Button disabled={step === 0} onClick={() => setStep((s) => s - 1)}>
              Quay lại
            </Button>
            {step < STEPS.length - 1 && (
              <Button type="primary" onClick={handleNext} disabled={loading}>
                Tiếp tục
              </Button>
            )}
          </span>
        </div>
      </div>

      {publishId !== null && (
        <AssignTargetsModal
          assignmentId={publishId}
          mode="publish"
          open
          dueAt={loaded?.dueAt}
          onClose={() => setPublishId(null)}
          onDone={() => router.push(assignmentPath(publishId))}
        />
      )}
    </div>
  );
}
