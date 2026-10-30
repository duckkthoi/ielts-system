'use client';

import { useEffect, useMemo, useState } from 'react';

import { Alert, Button, Skeleton } from 'antd';
import Link from 'next/link';

import { EssayAnnotator, studentAnnotationViews } from '@/components/EssayAnnotator';
import { PaperViewer } from '@/components/PaperEditor';
import { examApi } from '@/lib/api/exam';
import { studentApi } from '@/lib/api/student';
import { resultPath } from '@/lib/constants';
import type { CitationStyle } from '@/lib/types/assignment';
import type { ExamSession } from '@/lib/types/exam';
import type { CslItem } from '@/lib/types/paper';
import type { StudentInlineComment, StudentPartDetail } from '@/lib/types/student';
import { formatDateTime } from '@/lib/utils/date';
import { countWords } from '@/lib/utils/wordCount';

import { isWritable } from './const';
import { EditMode } from './EditMode';
import { EssayEditor } from './EssayEditor';
import { ExamStartCard } from './ExamStartCard';
import { Layout } from './Layout';
import { PaperEditMode } from './PaperEditMode';
import type { DraftInit } from './useDraftAutosave';
import { WordCounter } from './WordCounter';

// ─── Một phần đang làm: đề bài (trái 40%) + ô soạn (phải 60%) ─────────────────
// Còn soạn được (theo `canSubmit` của server) → EditMode có tự lưu + nộp; ngược lại → ViewMode chỉ đọc
// bản nộp gần nhất. Nộp xong / server báo không soạn được nữa → `onChanged` để trang tải lại trạng thái.
// Phần ở chế độ thi mà chưa bắt đầu → thẻ "Bắt đầu làm bài" thay cho ô soạn (BR-23).
// Bài PAPER còn soạn được → trình soạn tài liệu nhóm (PaperEditMode) thay cho ô soạn chữ thuần.

// ─── Chỉ đọc: đã nộp / quá hạn / bài đóng ─────────────────────────────────────

/** Bản nộp đã trả bài: bài viết kèm đánh dấu của giáo viên, chỉ đọc (cùng giao diện với màn chấm). */
function AnnotatedEssay({ content, inline }: { content: string; inline: StudentInlineComment[] }) {
  const [selected, setSelected] = useState<string | null>(null);
  const views = useMemo(() => studentAnnotationViews(inline), [inline]);
  return (
    <div className="mx-auto w-full max-w-240">
      <EssayAnnotator content={content} annotations={views} readOnly selectedKey={selected} onSelect={setSelected} />
    </div>
  );
}

type Submitted = {
  content: string;
  submittedAt: string;
  late: boolean;
  inline: StudentInlineComment[] | null;
  /** Bài PAPER: tài liệu có định dạng + tài liệu được trích lúc nộp (B25a). */
  paper: { contentJson: Record<string, unknown>; references: { id: number; csl: CslItem }[]; wordCount: number } | null;
};

function ViewMode({
  assignmentId,
  assignmentClosed,
  citationStyle,
  part,
  submitted
}: {
  assignmentId: number;
  assignmentClosed: boolean;
  citationStyle: CitationStyle;
  part: StudentPartDetail;
  submitted: Submitted | null;
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
              <Link href={resultPath(assignmentId, part.partId)}>
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
      {submitted?.paper ? (
        <PaperViewer
          contentJson={submitted.paper.contentJson}
          references={submitted.paper.references}
          style={citationStyle}
          footer={<span className="text-caption text-neutral-500">{submitted.paper.wordCount} từ</span>}
        />
      ) : submitted?.inline ? (
        <AnnotatedEssay content={submitted.content} inline={submitted.inline} />
      ) : submitted ? (
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
  | { mode: 'edit'; init: DraftInit; exam: ExamSession | null }
  | { mode: 'paper' }
  | { mode: 'view'; submitted: Submitted | null };

export function WritingPanel({
  assignmentId,
  assignmentClosed,
  paper,
  citationStyle,
  part,
  onChanged
}: {
  assignmentId: number;
  assignmentClosed: boolean;
  /** Bài PAPER (viết theo nhóm). */
  paper: boolean;
  /** Kiểu trích dẫn của bài PAPER (IEEE / APA). */
  citationStyle: CitationStyle | null;
  part: StudentPartDetail;
  onChanged: () => void;
}) {
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const writable = isWritable(part);

  useEffect(() => {
    let cancelled = false;
    const load = async (): Promise<Loaded> => {
      if (writable && paper) return { mode: 'paper' };
      if (writable) {
        const [d, exam] = await Promise.all([
          studentApi.draft(part.partId),
          part.examMode ? examApi.current(part.partId) : Promise.resolve(null)
        ]);
        return {
          mode: 'edit',
          init: { content: d.content, draftVersion: d.draftVersion, savedAt: d.savedAt, submissionId: d.submissionId },
          exam
        };
      }
      if (part.currentVersion === 0) return { mode: 'view', submitted: null };
      const v = await studentApi.version(part.partId, part.currentVersion);
      return {
        mode: 'view',
        submitted: {
          content: v.content,
          submittedAt: v.submittedAt,
          late: v.late,
          inline: v.result?.inlineComments ?? null,
          paper: v.contentJson
            ? { contentJson: v.contentJson, references: v.references ?? [], wordCount: v.wordCount }
            : null
        }
      };
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
  }, [part.partId, part.currentVersion, part.examMode, writable, paper]);

  if (!loaded) {
    return (
      <div className="mx-auto w-full max-w-180 p-6">
        <Skeleton active paragraph={{ rows: 10 }} />
      </div>
    );
  }
  if (loaded.mode === 'edit' && part.examMode && !loaded.exam) {
    const init = loaded.init;
    return (
      <Layout part={part} toolbar={null}>
        <ExamStartCard part={part} onStarted={(exam) => setLoaded({ mode: 'edit', init, exam })} />
      </Layout>
    );
  }
  if (loaded.mode === 'paper') {
    return (
      <PaperEditMode
        assignmentId={assignmentId}
        citationStyle={citationStyle ?? 'IEEE'}
        part={part}
        onChanged={onChanged}
      />
    );
  }
  return loaded.mode === 'edit' ? (
    <EditMode part={part} init={loaded.init} exam={loaded.exam} onChanged={onChanged} />
  ) : (
    <ViewMode
      assignmentId={assignmentId}
      assignmentClosed={assignmentClosed}
      citationStyle={citationStyle ?? 'IEEE'}
      part={part}
      submitted={loaded.submitted}
    />
  );
}
