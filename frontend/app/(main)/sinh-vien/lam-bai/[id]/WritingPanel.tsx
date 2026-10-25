'use client';

import { useEffect, useMemo, useState } from 'react';

import { Alert, App, Button, Collapse, Skeleton, Tooltip } from 'antd';
import { Send } from 'lucide-react';
import Link from 'next/link';

import { CountdownTimer } from '@/components/CountdownTimer';
import { studentApi } from '@/lib/api/student';
import { ApiError } from '@/lib/apiError';
import { studentAssignmentPath } from '@/lib/constants';
import type { StudentPartDetail } from '@/lib/types/student';
import { formatDateTime } from '@/lib/utils/date';
import { useOnlineStatus } from '@/lib/utils/hooks';
import { countWords } from '@/lib/utils/wordCount';
import { useAuthStore } from '@/stores/useAuthStore';

import { AutosaveIndicator } from './AutosaveIndicator';
import { isWritable } from './const';
import { DraftConflictModal } from './DraftConflictModal';
import { EssayEditor } from './EssayEditor';
import { PromptPanel } from './PromptPanel';
import { useDraftAutosave } from './useDraftAutosave';
import type { DraftInit } from './useDraftAutosave';
import { WordCounter } from './WordCounter';

// ─── Một phần đang làm: đề bài (trái 40%) + ô soạn (phải 60%) ─────────────────
// Còn soạn được (theo `canSubmit` của server) → EditMode có tự lưu + nộp; ngược lại → ViewMode chỉ đọc
// bản nộp gần nhất. Nộp xong / server báo không soạn được nữa → `onChanged` để trang tải lại trạng thái.

/** Lỗi nộp nghĩa là trạng thái trên trang đã cũ → tải lại. */
const RELOAD_CODES = new Set(['VERSION_CONFLICT', 'ALREADY_SUBMITTED', 'DEADLINE_PASSED', 'INVALID_STATE']);

function Layout({
  part,
  toolbar,
  children
}: {
  part: StudentPartDetail;
  toolbar: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
      <aside className="hidden border-r border-neutral-100 bg-neutral-0 lg:block">
        <div className="sticky top-16 max-h-[calc(100vh-4rem)] overflow-y-auto p-6">
          <h2 className="mb-3 text-h2 font-semibold text-neutral-900">{part.title}</h2>
          <PromptPanel part={part} />
        </div>
      </aside>
      <section className="flex min-w-0 flex-col gap-4 px-4 py-4 sm:px-6 lg:py-6">
        <Collapse
          className="bg-neutral-0 lg:hidden"
          items={[{ key: 'prompt', label: 'Xem đề bài', children: <PromptPanel part={part} /> }]}
        />
        {toolbar}
        {children}
      </section>
    </div>
  );
}

// ─── Đang soạn ────────────────────────────────────────────────────────────────

function EditMode({ part, init, onChanged }: { part: StudentPartDetail; init: DraftInit; onChanged: () => void }) {
  const { modal, message } = App.useApp();
  // Layout (main) chỉ render khi đã đăng nhập nên luôn có user; 0 chỉ để thoả kiểu.
  const userId = useAuthStore((s) => s.user?.id ?? 0);
  const online = useOnlineStatus();
  const draft = useDraftAutosave(userId, part.partId, init, onChanged);
  const [submitting, setSubmitting] = useState(false);
  const words = useMemo(() => countWords(draft.content), [draft.content]);
  const blank = words === 0;
  const canSend = online && draft.state !== 'OFFLINE';
  const short = part.minWords !== null && words < part.minWords;

  const doSubmit = async () => {
    setSubmitting(true);
    await draft.pause();
    try {
      const res = await studentApi.submit(part.partId, draft.content, part.currentVersion);
      draft.stop();
      message.success(`Đã nộp ${part.title} lúc ${formatDateTime(res.submittedAt)}${res.late ? ' (nộp muộn)' : ''}`);
      onChanged();
    } catch (err) {
      // axiosClient đã toast lý do; trạng thái cũ thì tải lại
      if (err instanceof ApiError && err.code && RELOAD_CODES.has(err.code)) onChanged();
    } finally {
      setSubmitting(false);
    }
  };

  const confirmSubmit = () =>
    modal.confirm({
      title: `Nộp ${part.title}?`,
      content: short
        ? `Bài có ${words} từ, ít hơn yêu cầu ${part.minWords} từ.`
        : 'Sau khi nộp sẽ không sửa được nữa, trừ khi giáo viên yêu cầu sửa.',
      okText: short ? 'Vẫn nộp' : 'Nộp bài',
      cancelText: 'Viết tiếp',
      onOk: doSubmit
    });

  const submitButton = (block: boolean) => (
    <Tooltip
      title={
        !canSend
          ? 'Cần có mạng để nộp bài'
          : blank
            ? 'Bài đang trống'
            : draft.conflict
              ? 'Chọn bản nháp trước khi nộp'
              : undefined
      }
    >
      <Button
        type="primary"
        size="large"
        block={block}
        icon={<Send size={16} />}
        loading={submitting}
        disabled={!canSend || blank || draft.conflict !== null}
        onClick={confirmSubmit}
      >
        {part.status === 'REVISION_REQUESTED' ? 'Nộp bản sửa' : 'Nộp phần này'}
      </Button>
    </Tooltip>
  );

  const indicator = (
    <AutosaveIndicator
      state={draft.state}
      savedAt={draft.savedAt}
      error={draft.error}
      onRetry={() => void draft.save()}
    />
  );

  const toolbar = (
    <div className="flex flex-col gap-3">
      {part.status === 'REVISION_REQUESTED' && (
        <Alert
          type="error"
          showIcon
          title={`Giáo viên yêu cầu sửa bài${part.revisionDueAt ? ` · Hạn sửa ${formatDateTime(part.revisionDueAt)}` : ''}`}
          description={part.revisionNote ? `Lời nhắn: ${part.revisionNote}` : undefined}
        />
      )}
      <div className="mx-auto flex w-full max-w-180 flex-wrap items-center justify-between gap-3">
        <span className="flex flex-wrap items-center gap-3">
          {part.effectiveDueAt && <CountdownTimer dueAt={part.effectiveDueAt} />}
          {indicator}
        </span>
        <span className="hidden lg:inline-flex">{submitButton(false)}</span>
      </div>
    </div>
  );

  return (
    <Layout part={part} toolbar={toolbar}>
      <EssayEditor
        value={draft.content}
        onChange={draft.setContent}
        footer={
          <>
            <WordCounter count={words} minWords={part.minWords} />
            {indicator}
          </>
        }
      />
      {/* Điện thoại: thanh dưới cố định với số từ + nút nộp */}
      <div className="sticky bottom-0 -mx-4 flex items-center gap-3 border-t border-neutral-100 bg-neutral-0 px-4 py-3 sm:-mx-6 sm:px-6 lg:hidden">
        <WordCounter count={words} minWords={part.minWords} />
        <span className="ml-auto">{submitButton(false)}</span>
      </div>
      <DraftConflictModal conflict={draft.conflict} localContent={draft.content} onResolve={draft.resolveConflict} />
    </Layout>
  );
}

// ─── Chỉ đọc: đã nộp / quá hạn / bài đóng ─────────────────────────────────────

function ViewMode({
  assignmentId,
  assignmentClosed,
  part,
  submitted
}: {
  assignmentId: number;
  assignmentClosed: boolean;
  part: StudentPartDetail;
  submitted: { content: string; submittedAt: string; late: boolean } | null;
}) {
  const pending = part.status === 'NOT_STARTED' || part.status === 'DRAFT' || part.status === 'REVISION_REQUESTED';
  const toolbar = (
    <div className="mx-auto flex w-full max-w-180 flex-col gap-3">
      {pending ? (
        <Alert
          type="error"
          showIcon
          title={
            assignmentClosed
              ? 'Bài tập đã đóng, không nộp được nữa.'
              : 'Đã quá hạn nộp. Liên hệ giáo viên nếu cần gia hạn.'
          }
        />
      ) : submitted ? (
        <Alert
          type="success"
          showIcon
          title={`Đã nộp lúc ${formatDateTime(submitted.submittedAt)}${submitted.late ? ' · nộp muộn' : ''}`}
          description={part.status === 'RETURNED' ? 'Giáo viên đã trả bài.' : 'Bài đang chờ giáo viên chấm.'}
          action={
            part.status === 'RETURNED' ? (
              <Link href={studentAssignmentPath(assignmentId)}>
                <Button size="small">Xem kết quả</Button>
              </Link>
            ) : undefined
          }
        />
      ) : null}
    </div>
  );

  return (
    <Layout part={part} toolbar={toolbar}>
      {submitted ? (
        <EssayEditor
          value={submitted.content}
          readOnly
          footer={<WordCounter count={countWords(submitted.content)} minWords={part.minWords} />}
        />
      ) : (
        <p className="mx-auto w-full max-w-180 text-neutral-500">Bạn chưa nộp phần này.</p>
      )}
    </Layout>
  );
}

// ─── Nạp dữ liệu theo chế độ ──────────────────────────────────────────────────

type Loaded =
  | { mode: 'edit'; init: DraftInit }
  | { mode: 'view'; submitted: { content: string; submittedAt: string; late: boolean } | null };

export function WritingPanel({
  assignmentId,
  assignmentClosed,
  part,
  onChanged
}: {
  assignmentId: number;
  assignmentClosed: boolean;
  part: StudentPartDetail;
  onChanged: () => void;
}) {
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const writable = isWritable(part);

  useEffect(() => {
    let cancelled = false;
    const load = async (): Promise<Loaded> => {
      if (writable) {
        const d = await studentApi.draft(part.partId);
        return { mode: 'edit', init: { content: d.content, draftVersion: d.draftVersion, savedAt: d.savedAt } };
      }
      if (part.currentVersion === 0) return { mode: 'view', submitted: null };
      const v = await studentApi.version(part.partId, part.currentVersion);
      return { mode: 'view', submitted: { content: v.content, submittedAt: v.submittedAt, late: v.late } };
    };
    load()
      .then((data) => {
        if (!cancelled) setLoaded(data);
      })
      .catch(() => {
        // axiosClient đã toast lỗi; hiện chỉ đọc trống để không mất trang
        if (!cancelled) setLoaded({ mode: 'view', submitted: null });
      });
    return () => {
      cancelled = true;
    };
  }, [part.partId, part.currentVersion, writable]);

  if (!loaded) {
    return (
      <div className="mx-auto w-full max-w-180 p-6">
        <Skeleton active paragraph={{ rows: 10 }} />
      </div>
    );
  }
  return loaded.mode === 'edit' ? (
    <EditMode part={part} init={loaded.init} onChanged={onChanged} />
  ) : (
    <ViewMode
      assignmentId={assignmentId}
      assignmentClosed={assignmentClosed}
      part={part}
      submitted={loaded.submitted}
    />
  );
}
