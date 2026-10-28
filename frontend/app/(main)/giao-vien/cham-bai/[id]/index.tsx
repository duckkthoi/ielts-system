'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';

import { App, Button, Result, Segmented, Select, Skeleton } from 'antd';
import { ChevronLeft, ChevronRight, MessageSquare } from 'lucide-react';
import Link from 'next/link';
import { useParams, useRouter, useSearchParams } from 'next/navigation';

import { BandBadge } from '@/components/BandBadge';
import type { AnnotateAction } from '@/components/EssayAnnotator';
import { StatusTag } from '@/components/StatusTag';
import { feedbackApi } from '@/lib/api/feedback';
import { gradingApi } from '@/lib/api/grading';
import { gradingPath, queueQueryString } from '@/lib/constants';
import { getSubmissionDisplayStatus, SUBMISSION_STATUS_META } from '@/lib/theme/status';
import type { ErrorCodeDto, GradingScreen, QueueParams, QueueStatus, ZeroReason } from '@/lib/types/grading';
import { formatDate } from '@/lib/utils/date';

import { AnnotationFormModal } from './AnnotationFormModal';
import type { AnnotationFormTarget } from './AnnotationFormModal';
import { AnnotationList } from './AnnotationList';
import { ComparePanel } from './ComparePanel';
import { QUEUE_WINDOW, toAnnotationView } from './const';
import { EssayPanel } from './EssayPanel';
import { GradingPanel } from './GradingPanel';
import { RevisionModal } from './RevisionModal';
import { useGradeForm } from './useGradeForm';
import { ZeroModal } from './ZeroModal';

// ─── Giáo viên: màn chấm một bài nộp (UC-17, UC-18, SD-08, Prompt 6 B) ────────
// /giao-vien/cham-bai/{submissionId}?versionNo&<bộ lọc hàng đợi>. Bài làm 58% bên trái, bảng chấm 42% bên phải
// (cuộn riêng); dưới 1024px thành 2 tab "Bài làm" / "Chấm điểm". Trả bài / yêu cầu sửa xong → sang bài kế tiếp.

const QUEUE_PATH = '/giao-vien/cham-bai';
const num = (v: string | null) => (v && /^\d+$/.test(v) ? Number(v) : undefined);

// ─── Vùng làm việc của một version (dựng lại khi đổi version / sau khi trả bài) ─

function Workspace({
  screen,
  nextHref,
  onScreen
}: {
  screen: GradingScreen;
  nextHref: string | null;
  onScreen: (s: GradingScreen) => void;
}) {
  const router = useRouter();
  const { message } = App.useApp();
  const g = useGradeForm(screen);
  const [tab, setTab] = useState<'essay' | 'grade'>('grade');
  const [busy, setBusy] = useState(false);
  const [zeroOpen, setZeroOpen] = useState(false);
  const [revisionOpen, setRevisionOpen] = useState(false);
  const [rightTab, setRightTab] = useState<'grade' | 'marks'>('grade');
  const [codes, setCodes] = useState<ErrorCodeDto[]>([]);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [scrollNonce, setScrollNonce] = useState(0);
  const [formTarget, setFormTarget] = useState<AnnotationFormTarget | null>(null);
  const [leftMode, setLeftMode] = useState<'essay' | 'compare'>('essay');
  const content = screen.version.content;
  const hasOlder = screen.versions.some((v) => v.versionNo < screen.version.versionNo);

  useEffect(() => {
    feedbackApi
      .errorCodes()
      .then(setCodes)
      .catch(() => undefined);
  }, []);

  const views = useMemo(
    () => g.form.inline.map((ic) => toAnnotationView(ic, codes, screen.criteria)),
    [g.form.inline, codes, screen.criteria]
  );

  // ─── Đánh dấu trên bài ──────────────────────────────────────────────────────
  // Tô màu / dùng hay / xoá chữ: thêm ngay; các nút còn lại mở form.
  const handleAction = (action: AnnotateAction, range: { start: number; end: number }) => {
    const quote = content.slice(range.start, range.end);
    const base = { startOffset: range.start, endOffset: range.end, quote, errorCode: null, criterionId: null };
    const quick = { comment: null, suggestion: null, edit: null };
    if (action === 'highlight') setSelectedKey(g.addInline({ ...base, ...quick, kind: 'HIGHLIGHT' }));
    else if (action === 'good') setSelectedKey(g.addInline({ ...base, ...quick, kind: 'GOOD' }));
    else if (action === 'delete') setSelectedKey(g.addInline({ ...base, ...quick, kind: 'ERROR', edit: 'DELETE' }));
    else setFormTarget({ range, quote, action });
  };

  const handleEditAnn = (key: string) => {
    const ic = g.form.inline.find((x) => x.key === key);
    if (!ic) return;
    const { key: k, ...draft } = ic;
    setFormTarget({ range: { start: ic.startOffset, end: ic.endOffset }, quote: ic.quote, key: k, draft });
  };

  const handleDeleteAnn = (key: string) => {
    g.removeInline(key);
    setSelectedKey(null);
  };

  const pickFromList = (key: string) => {
    setSelectedKey(key);
    setScrollNonce((n) => n + 1);
    setTab('essay');
    setLeftMode('essay');
  };

  const finish = (updated: GradingScreen, text: string) => {
    message.success(text);
    if (nextHref) router.push(nextHref);
    else onScreen(updated);
  };

  const handleReturn = async () => {
    setBusy(true);
    try {
      if (!(await g.flush())) return;
      finish(await gradingApi.returnGrade(screen.submission.id), 'Đã trả bài cho sinh viên');
    } catch {
      // axiosClient đã toast lý do (vd còn tiêu chí chưa chấm)
    } finally {
      setBusy(false);
    }
  };

  const handleRevision = async (note: string | null, dueAt: string) => {
    if (!(await g.flush())) throw new Error('save');
    const updated = await gradingApi.requestRevision(screen.submission.id, { note, dueAt });
    setRevisionOpen(false);
    finish(updated, 'Đã gửi yêu cầu sửa cho sinh viên');
  };

  const handleZero = (reason: ZeroReason) => {
    g.setZeroReason(reason);
    setZeroOpen(false);
  };

  return (
    <>
      <div className="border-b border-neutral-100 bg-neutral-0 px-4 py-2 lg:hidden">
        <Segmented
          block
          value={tab}
          onChange={(v) => setTab(v as 'essay' | 'grade')}
          options={[
            { value: 'essay', label: 'Bài làm' },
            { value: 'grade', label: 'Chấm điểm' }
          ]}
        />
      </div>
      <div className="grid min-h-0 flex-1 grid-cols-1 lg:grid-cols-[minmax(0,58fr)_minmax(0,42fr)]">
        <section className={`min-h-0 overflow-y-auto p-4 sm:p-6 ${tab === 'essay' ? '' : 'hidden lg:block'}`}>
          {hasOlder && (
            <Segmented
              className="mb-3"
              value={leftMode}
              onChange={(v) => setLeftMode(v as 'essay' | 'compare')}
              options={[
                { value: 'essay', label: `Bài làm lần ${screen.version.versionNo}` },
                { value: 'compare', label: 'So sánh với lần trước' }
              ]}
            />
          )}
          {leftMode === 'compare' && hasOlder ? (
            <ComparePanel screen={screen} codes={codes} />
          ) : (
            <EssayPanel
              screen={screen}
              annotations={views}
              selectedKey={selectedKey}
              scrollNonce={scrollNonce}
              onSelect={setSelectedKey}
              onAction={handleAction}
              onEdit={handleEditAnn}
              onDelete={handleDeleteAnn}
            />
          )}
        </section>
        <aside
          className={`min-h-0 overflow-y-auto border-l border-neutral-100 bg-neutral-25 ${tab === 'grade' ? '' : 'hidden lg:block'}`}
        >
          <div className="sticky top-0 z-10 border-b border-neutral-100 bg-neutral-25 px-4 pt-3 pb-2 sm:px-5">
            <Segmented
              block
              value={rightTab}
              onChange={(v) => setRightTab(v as 'grade' | 'marks')}
              options={[
                { value: 'grade', label: 'Chấm điểm' },
                { value: 'marks', label: `Đánh dấu (${views.length})` }
              ]}
            />
          </div>
          {rightTab === 'grade' ? (
            <GradingPanel
              screen={screen}
              g={g}
              busy={busy}
              onZero={() => setZeroOpen(true)}
              onReturn={() => void handleReturn()}
              onRevision={() => setRevisionOpen(true)}
            />
          ) : (
            <AnnotationList annotations={views} errorCodes={codes} selectedKey={selectedKey} onPick={pickFromList} />
          )}
        </aside>
      </div>
      {formTarget && (
        <AnnotationFormModal
          target={formTarget}
          errorCodes={codes}
          criteria={screen.criteria}
          onCancel={() => setFormTarget(null)}
          onSave={(draft) => {
            if (formTarget.key) g.updateInline(formTarget.key, draft);
            else setSelectedKey(g.addInline(draft));
            setFormTarget(null);
          }}
        />
      )}
      <ZeroModal open={zeroOpen} onCancel={() => setZeroOpen(false)} onConfirm={handleZero} />
      <RevisionModal
        open={revisionOpen}
        revisionsLeft={screen.assignment.maxRevisions - screen.submission.revisionsUsed}
        onCancel={() => setRevisionOpen(false)}
        onSubmit={handleRevision}
      />
    </>
  );
}

// ─── Trang ────────────────────────────────────────────────────────────────────

export default function GradingPage() {
  const router = useRouter();
  const id = Number(useParams<{ id: string }>().id);
  const sp = useSearchParams();
  const versionNo = num(sp.get('versionNo'));
  const filter = useMemo<QueueParams>(
    () => ({
      status: (sp.get('status') as QueueStatus | null) ?? undefined,
      assignmentId: num(sp.get('assignmentId')),
      partId: num(sp.get('partId')),
      classId: num(sp.get('classId')),
      keyword: sp.get('keyword') ?? undefined
    }),
    [sp]
  );
  const filterQs = queueQueryString(filter);

  const [loaded, setLoaded] = useState<{ key: string; screen: GradingScreen } | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [queueIds, setQueueIds] = useState<number[] | null>(null);

  useEffect(() => {
    let cancelled = false;
    gradingApi
      .screen(id, versionNo)
      .then((s) => {
        if (!cancelled) setLoaded({ key: `${id}-${versionNo ?? 'new'}`, screen: s });
      })
      .catch(() => {
        if (!cancelled) setNotFound(true);
      });
    return () => {
      cancelled = true;
    };
  }, [id, versionNo]);

  // Vị trí trong hàng đợi (cùng bộ lọc với trang hàng đợi) cho "Trước / Sau".
  useEffect(() => {
    let cancelled = false;
    gradingApi
      .queue({ ...filter, page: 0, size: QUEUE_WINDOW })
      .then((res) => {
        if (!cancelled) setQueueIds(res.data.map((r) => r.submissionId));
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
    // filterQs đại diện cho filter
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterQs]);

  const onScreen = useCallback((s: GradingScreen) => {
    setLoaded({ key: `${s.submission.id}-${s.version.versionNo}-${Date.now()}`, screen: s });
  }, []);

  const backHref = filterQs ? `${QUEUE_PATH}?${filterQs}` : QUEUE_PATH;

  if (notFound) {
    return (
      <Result
        status="404"
        title="Không tìm thấy bài nộp"
        subTitle="Bài nộp không tồn tại hoặc không thuộc bài tập của bạn."
        extra={<Link href={backHref}>Về hàng đợi chấm</Link>}
      />
    );
  }
  if (!loaded) {
    return (
      <div className="p-6">
        <Skeleton active paragraph={{ rows: 12 }} />
      </div>
    );
  }

  const { screen } = loaded;
  const idx = queueIds ? queueIds.indexOf(id) : -1;
  const prevId = idx > 0 ? queueIds![idx - 1] : null;
  const nextId = idx >= 0 && queueIds && idx < queueIds.length - 1 ? queueIds[idx + 1] : null;
  const status = getSubmissionDisplayStatus(screen.submission.status, false, 'teacher');

  return (
    <div className="flex flex-col lg:h-[calc(100vh-4rem)]">
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-100 bg-neutral-0 px-4 py-3 sm:px-6">
        <div className="min-w-0">
          <h1 className="truncate text-h3 font-semibold text-neutral-900">
            {screen.submission.student.fullName} ·{' '}
            <span className="font-mono">{screen.submission.student.studentCode}</span>
          </h1>
          <p className="truncate text-small text-neutral-500">
            {screen.assignment.title} · {screen.part.title}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Select
            className="min-w-56"
            value={screen.version.versionNo}
            onChange={(v: number) => {
              const qs = new URLSearchParams(filterQs);
              qs.set('versionNo', String(v));
              router.replace(`/giao-vien/cham-bai/${id}?${qs}`, { scroll: false });
            }}
            options={screen.versions.map((v) => ({
              value: v.versionNo,
              label: (
                <span className="inline-flex items-center gap-2">
                  Lần {v.versionNo}
                  {v.versionNo === screen.submission.currentVersion && ' (mới nhất)'} · {formatDate(v.submittedAt)}
                  {v.band !== null && <BandBadge band={v.band} size="sm" />}
                </span>
              )
            }))}
          />
          {screen.version.late && <StatusTag meta={SUBMISSION_STATUS_META.LATE} size="sm" />}
          <StatusTag meta={SUBMISSION_STATUS_META[status]} size="sm" />
          <Link href={`/chat?kenh=submission-${id}`}>
            <Button icon={<MessageSquare size={16} />}>Trao đổi</Button>
          </Link>
          <span className="inline-flex items-center gap-1">
            <Button
              icon={<ChevronLeft size={16} />}
              disabled={!prevId}
              onClick={() => prevId && router.push(gradingPath(prevId, filter))}
            >
              Trước
            </Button>
            <Button disabled={!nextId} onClick={() => nextId && router.push(gradingPath(nextId, filter))}>
              Sau <ChevronRight size={16} />
            </Button>
            {idx >= 0 && queueIds && (
              <span className="px-1 text-small text-neutral-500 tabular-nums">
                {idx + 1}/{queueIds.length}
              </span>
            )}
          </span>
        </div>
      </header>
      <Workspace
        key={loaded.key}
        screen={screen}
        nextHref={nextId ? gradingPath(nextId, filter) : null}
        onScreen={onScreen}
      />
    </div>
  );
}
