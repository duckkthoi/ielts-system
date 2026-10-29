'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';

import { Alert, App, Button, Result, Skeleton, Tooltip } from 'antd';
import { BookMarked, Send, Users } from 'lucide-react';
import Link from 'next/link';

import { CountdownTimer } from '@/components/CountdownTimer';
import { PaperEditor, PaperOutline } from '@/components/PaperEditor';
import { ReferenceLibrary, useGroupReferences } from '@/components/ReferenceLibrary';
import { studentApi } from '@/lib/api/student';
import { ApiError } from '@/lib/apiError';
import { studentAssignmentPath } from '@/lib/constants';
import { subscribeRealtime } from '@/lib/realtime';
import type { CitationStyle } from '@/lib/types/assignment';
import type { StudentPartDetail } from '@/lib/types/student';
import { formatDateTime } from '@/lib/utils/date';
import { useOnlineStatus } from '@/lib/utils/hooks';
import { initialDoc, sectionWordCounts, type DocNode } from '@/lib/utils/paperDoc';

import { RELOAD_CODES } from './const';
import { Layout } from './Layout';
import { PaperSyncIndicator } from './PaperSyncIndicator';
import { usePaperDocument } from './usePaperDocument';

// ─── Bài PAPER: cả nhóm viết chung một tài liệu cho giai đoạn này (BR-28) ─────
// Thay đổi lưu liên tục qua Yjs (không có nút lưu). Nộp (B25a): lưu snapshot tới thay đổi mới nhất rồi gọi nộp —
// server nộp chính snapshot đó và chặn mục bắt buộc còn trống (BR-29); thành viên khác vừa gõ (SNAPSHOT_STALE) thì
// lưu lại snapshot và thử đúng một lần nữa.

export function PaperEditMode({
  assignmentId,
  citationStyle,
  part,
  onChanged
}: {
  assignmentId: number;
  citationStyle: CitationStyle;
  part: StudentPartDetail;
  onChanged: () => void;
}) {
  const { modal, message } = App.useApp();
  const online = useOnlineStatus();
  const mirrorDraft = useCallback(
    async (contentText: string) => {
      // Bài Paper dùng một bản nháp chung của nhóm; bỏ kiểm tra draftVersion để
      // hai thành viên có thể tự lưu nối tiếp nhau mà không bật xung đột giả.
      await studentApi.saveDraft(part.partId, contentText, null);
    },
    [part.partId]
  );
  const paper = usePaperDocument(part.partId, part.sections, mirrorDraft);
  const [json, setJson] = useState<DocNode | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [libraryOpen, setLibraryOpen] = useState(false);
  const library = useGroupReferences(paper.groupId);

  // Chưa nạp xong tài liệu: mục lục theo khung, mọi mục 0 từ.
  const counts = useMemo(
    () => sectionWordCounts(json ?? initialDoc(part.sections), part.sections),
    [json, part.sections]
  );
  const words = counts.reduce((sum, c) => sum + c.words, 0);
  const emptyRequired = counts.filter((c) => c.required && c.words === 0);
  const canSend = online && paper.status === 'synced' && paper.editable;

  // Lỗi của lệnh STOMP vừa gửi (vd update quá lớn, nhóm đã nộp) — server chỉ gửi riêng người gửi.
  useEffect(
    () =>
      subscribeRealtime('/user/queue/errors', (payload) => {
        const err = payload as { errorMessage?: string };
        if (err.errorMessage) message.error(err.errorMessage);
      }),
    [message]
  );

  if (paper.error === 'NO_GROUP') {
    return (
      <Layout part={part} toolbar={null}>
        <Result
          icon={<Users size={48} className="mx-auto text-brand-600" />}
          title="Bạn chưa có nhóm"
          subTitle="Bài này viết theo nhóm. Hãy vào hoặc lập nhóm ở trang chi tiết bài rồi quay lại."
          extra={
            <Link href={studentAssignmentPath(assignmentId)}>
              <Button type="primary">Đến trang chi tiết bài</Button>
            </Link>
          }
        />
      </Layout>
    );
  }
  if (paper.error === 'FAILED') {
    return (
      <Layout part={part} toolbar={null}>
        <Result
          status="error"
          title="Không mở được tài liệu của nhóm"
          subTitle="Kiểm tra kết nối mạng rồi thử lại."
          extra={<Button onClick={paper.retry}>Thử lại</Button>}
        />
      </Layout>
    );
  }

  const send = async () => {
    await paper.snapshotNow(true);
    return studentApi.submit(part.partId, null, part.currentVersion, false);
  };

  const doSubmit = async () => {
    setSubmitting(true);
    try {
      let res;
      try {
        res = await send();
      } catch (err) {
        if (!(err instanceof ApiError && err.code === 'SNAPSHOT_STALE')) throw err;
        res = await send();
      }
      message.success(`Đã nộp ${part.title} lúc ${formatDateTime(res.submittedAt)}${res.late ? ' (nộp muộn)' : ''}`);
      onChanged();
    } catch (err) {
      // Nộp gọi với toastError = false nên tự báo lý do; trạng thái cũ thì tải lại.
      if (err instanceof ApiError) {
        message.error(err.message);
        if (err.code && RELOAD_CODES.has(err.code)) onChanged();
      }
    } finally {
      setSubmitting(false);
    }
  };

  const confirmSubmit = () =>
    emptyRequired.length
      ? modal.warning({
          title: 'Chưa nộp được',
          content: `Mục bắt buộc còn trống: ${emptyRequired.map((s) => s.title).join(', ')}. Hãy viết các mục này trước khi nộp.`,
          okText: 'Viết tiếp'
        })
      : modal.confirm({
          title: `Nộp ${part.title} cho cả nhóm?`,
          content:
            'Bản nộp là tài liệu hiện tại của nhóm. Sau khi nộp, cả nhóm không sửa được nữa, trừ khi giáo viên yêu cầu sửa.',
          okText: 'Nộp bài',
          cancelText: 'Viết tiếp',
          onOk: doSubmit
        });

  const submitButton = (
    <Tooltip
      title={
        !online || paper.status === 'offline'
          ? 'Cần có mạng để nộp bài'
          : !canSend
            ? 'Chờ lưu xong thay đổi'
            : words === 0
              ? 'Tài liệu đang trống'
              : undefined
      }
    >
      <Button
        type="primary"
        size="large"
        icon={<Send size={16} />}
        loading={submitting}
        disabled={!canSend || words === 0}
        onClick={confirmSubmit}
      >
        {part.status === 'REVISION_REQUESTED' ? 'Nộp bản sửa' : 'Nộp giai đoạn này'}
      </Button>
    </Tooltip>
  );

  const openLibrary = () => {
    library.reload();
    setLibraryOpen(true);
  };

  const indicator = <PaperSyncIndicator status={paper.status} onRetry={paper.retry} />;
  const wordLabel = <span className="text-caption text-neutral-500">{words} từ</span>;

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
      {!paper.editable && paper.sync && (
        <Alert type="info" showIcon title="Tài liệu chỉ đọc: nhóm đã nộp giai đoạn này hoặc bài đã đóng." />
      )}
      <div className="mx-auto flex w-full max-w-240 flex-wrap items-center justify-between gap-3">
        <span className="flex flex-wrap items-center gap-3">
          {part.effectiveDueAt && <CountdownTimer dueAt={part.effectiveDueAt} />}
          {indicator}
          {paper.groupId !== null && (
            <Button size="small" icon={<BookMarked size={14} />} onClick={openLibrary}>
              Tài liệu tham khảo
            </Button>
          )}
        </span>
        <span className="hidden lg:inline-flex">{submitButton}</span>
      </div>
    </div>
  );

  return (
    <Layout part={part} toolbar={toolbar} outline={<PaperOutline counts={counts} />}>
      <p className="mx-auto -mb-2 flex w-full max-w-240 items-center gap-1.5 text-caption text-neutral-500">
        <Users size={14} />
        Cả nhóm cùng viết tài liệu này; thay đổi được lưu tự động.
      </p>
      {paper.sync ? (
        <PaperEditor
          doc={paper.sync.doc}
          editable={paper.editable}
          citations={{
            style: citationStyle,
            refs: library.refs ?? [],
            onOpenLibrary: openLibrary,
            onReload: library.reload
          }}
          onChange={(j) => setJson(j as DocNode)}
          footer={
            <>
              {wordLabel}
              {indicator}
            </>
          }
        />
      ) : (
        <div className="mx-auto w-full max-w-240 rounded-lg border border-neutral-100 bg-neutral-0 p-6">
          <Skeleton active paragraph={{ rows: 10 }} />
        </div>
      )}
      {/* Điện thoại: thanh dưới cố định với số từ + nút nộp */}
      <div className="sticky bottom-0 -mx-4 flex items-center gap-3 border-t border-neutral-100 bg-neutral-0 px-4 py-3 sm:-mx-6 sm:px-6 lg:hidden">
        {wordLabel}
        <span className="ml-auto">{submitButton}</span>
      </div>
      {paper.groupId !== null && (
        <ReferenceLibrary
          open={libraryOpen}
          onClose={() => setLibraryOpen(false)}
          groupId={paper.groupId}
          style={citationStyle}
          editable={paper.editable}
          refs={library.refs}
          failed={library.failed}
          reload={library.reload}
        />
      )}
    </Layout>
  );
}
