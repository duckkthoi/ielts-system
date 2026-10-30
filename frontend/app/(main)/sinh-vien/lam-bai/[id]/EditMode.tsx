'use client';

import { useMemo, useState } from 'react';

import { Alert, App, Button, Tooltip } from 'antd';
import { Eye, Send } from 'lucide-react';

import { CountdownTimer } from '@/components/CountdownTimer';
import { studentApi } from '@/lib/api/student';
import { ApiError } from '@/lib/apiError';
import type { ExamSession } from '@/lib/types/exam';
import type { StudentPartDetail } from '@/lib/types/student';
import { formatDateTime } from '@/lib/utils/date';
import { useOnlineStatus } from '@/lib/utils/hooks';
import { countWords } from '@/lib/utils/wordCount';
import { useAuthStore } from '@/stores/useAuthStore';

import { AutosaveIndicator } from './AutosaveIndicator';
import { RELOAD_CODES } from './const';
import { DraftConflictModal } from './DraftConflictModal';
import { EssayEditor } from './EssayEditor';
import { ExamClock } from './ExamClock';
import { Layout } from './Layout';
import { LiveCommentPanel } from './LiveCommentPanel';
import { useDraftAutosave } from './useDraftAutosave';
import type { DraftInit } from './useDraftAutosave';
import { useExamSignals, useLiveContent } from './useLiveSignals';
import { WordCounter } from './WordCounter';

// ─── Đang soạn: tự lưu + nộp; gửi bản đang gõ cho giáo viên xem trực tiếp (BR-26) ─
// Chế độ thi: đồng hồ tới `endsAt` (hết giờ vẫn soạn / nộp được — BR-24) và báo rời trang / dán (BR-25).

export function EditMode({
  part,
  init,
  exam,
  onChanged
}: {
  part: StudentPartDetail;
  init: DraftInit;
  /** Phiên thi đã bắt đầu (phần ở chế độ thi); phần thường → null. */
  exam: ExamSession | null;
  onChanged: () => void;
}) {
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
  useLiveContent(part.partId, draft.content);
  const onPaste = useExamSignals(exam?.sessionId ?? null);

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
          {exam && <ExamClock endsAt={exam.endsAt} />}
          {part.effectiveDueAt && <CountdownTimer dueAt={part.effectiveDueAt} />}
          {indicator}
        </span>
        <span className="hidden lg:inline-flex">{submitButton(false)}</span>
      </div>
    </div>
  );

  return (
    <Layout part={part} toolbar={toolbar}>
      <p className="mx-auto -mb-2 flex w-full max-w-180 items-center gap-1.5 text-caption text-neutral-500">
        <Eye size={14} />
        Giáo viên có thể xem trực tiếp bài bạn đang viết{exam ? ' · rời trang hoặc dán văn bản sẽ được ghi lại' : ''}.
      </p>
      <EssayEditor
        value={draft.content}
        onChange={draft.setContent}
        onPaste={exam ? onPaste : undefined}
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
      <LiveCommentPanel submissionId={draft.submissionId} />
      <DraftConflictModal conflict={draft.conflict} localContent={draft.content} onResolve={draft.resolveConflict} />
    </Layout>
  );
}
