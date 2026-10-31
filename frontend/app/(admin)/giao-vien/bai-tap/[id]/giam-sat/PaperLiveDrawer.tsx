'use client';

import { useMemo, useState } from 'react';

import { App, Button, Drawer, Result, Skeleton } from 'antd';
import { AlertTriangle, MessageSquarePlus, Sparkles } from 'lucide-react';

import {
  PaperEditor,
  PresenceAvatars,
  usePaperPresence,
  type SelectionAction,
  type TextSelection
} from '@/components/PaperEditor';
import {
  LiveCommentFormModal,
  LiveCommentList,
  useCommentSelection,
  useLiveComments,
  type LiveCommentFormValues
} from '@/components/PaperLiveComments';
import { paperLiveCommentApi } from '@/lib/api/paperLiveComment';
import type { SectionDto } from '@/lib/types/assignment';
import type { LiveComment, TeacherPaperDocument } from '@/lib/types/paper';
import { formatRelative } from '@/lib/utils/date';
import { initialDoc, sectionWordCounts, type DocNode } from '@/lib/utils/paperDoc';
import { useAuthStore } from '@/stores/useAuthStore';

import { useTeacherPaperDoc } from './useTeacherPaperDoc';

// ─── Giáo viên xem Paper nhóm đang viết + nhận xét / chỉ lỗi trực tiếp (H4) ───
// Phản hồi hướng dẫn trong lúc viết, không phải chấm chính thức: không có rubric / checklist, không sửa được nội dung.
// Bôi đen đoạn văn → Nhận xét · Chỉ lỗi · Gợi ý sửa · Bỏ chọn → form (đoạn chọn chỉ đọc, loại phản hồi, nội dung).
// Sinh viên thấy đoạn được tô + thẻ nhận xét ngay (STOMP `/topic/paper/{id}/comments`).

const ACTIONS: SelectionAction[] = [
  { key: 'comment', label: 'Nhận xét', icon: <MessageSquarePlus size={14} /> },
  { key: 'error', label: 'Chỉ lỗi', icon: <AlertTriangle size={14} className="text-error" /> },
  { key: 'suggest', label: 'Gợi ý sửa', icon: <Sparkles size={14} className="text-success" /> }
];

const INITIAL_BY_ACTION: Record<string, LiveCommentFormValues> = {
  comment: { kind: 'COMMENT', comment: '' },
  error: { kind: 'GRAMMAR', comment: '' },
  suggest: { kind: 'COMMENT', comment: '', suggestion: '' }
};

type FormState =
  | { mode: 'create'; selection: TextSelection; initial: LiveCommentFormValues }
  | { mode: 'edit'; comment: LiveComment; initial: LiveCommentFormValues };

export function PaperLiveDrawer({
  target,
  sections,
  onClose
}: {
  target: TeacherPaperDocument | null;
  sections: SectionDto[];
  onClose: () => void;
}) {
  const { message } = App.useApp();
  const me = useAuthStore((s) => s.user?.id);
  const documentId = target?.documentId ?? null;
  const doc = useTeacherPaperDoc(documentId);
  const peers = usePaperPresence(documentId, { announce: false });
  const live = useLiveComments(documentId);
  const selection = useCommentSelection(live.comments);
  const [json, setJson] = useState<DocNode | null>(null);
  const [form, setForm] = useState<FormState | null>(null);

  const sectionTitle = (key: string | null) => sections.find((s) => s.sectionKey === key)?.title;
  const words = useMemo(
    () => sectionWordCounts(json ?? initialDoc(sections), sections).reduce((sum, c) => sum + c.words, 0),
    [json, sections]
  );
  const editing = peers.filter((p) => p.section);

  const layer = useMemo(
    () => ({
      ...selection.layer,
      actions: ACTIONS,
      onAction: (key: string, sel: TextSelection) =>
        setForm({ mode: 'create', selection: sel, initial: INITIAL_BY_ACTION[key] ?? INITIAL_BY_ACTION.comment })
    }),
    [selection.layer]
  );

  const submit = async (values: LiveCommentFormValues) => {
    if (!form || documentId === null) return;
    try {
      if (form.mode === 'create') {
        const res = await paperLiveCommentApi.create(documentId, {
          startOffset: form.selection.start,
          endOffset: form.selection.end,
          quote: form.selection.quote,
          sectionKey: form.selection.sectionKey,
          ...values
        });
        live.upsert(res.data);
        message.success('Đã gửi nhận xét — nhóm thấy ngay');
      } else {
        const res = await paperLiveCommentApi.update(documentId, form.comment.id, values);
        live.upsert(res.data);
        message.success(res.message ?? 'Đã sửa nhận xét');
      }
      setForm(null);
    } catch {
      // axiosClient đã báo lỗi; giữ form để thử lại
    }
  };

  const remove = async (c: LiveComment) => {
    if (documentId === null) return;
    try {
      await paperLiveCommentApi.remove(documentId, c.id);
      live.drop(c.id);
    } catch {
      // đã báo lỗi
    }
  };

  const toggleResolved = async (c: LiveComment) => {
    if (documentId === null) return;
    try {
      live.upsert((await paperLiveCommentApi.resolve(documentId, c.id, !c.resolvedAt)).data);
    } catch {
      // đã báo lỗi
    }
  };

  const lastChange = doc.lastChangeAt ?? target?.updatedAt;

  return (
    <Drawer
      open={target !== null}
      onClose={onClose}
      size="min(1280px, 100vw)"
      title={
        target && (
          <span className="flex flex-col">
            <span>
              {target.groupName} · {target.partTitle}
            </span>
            <span className="text-small font-normal text-neutral-500">{target.members ?? 'Chưa có thành viên'}</span>
          </span>
        )
      }
      extra={<PresenceAvatars peers={peers} sectionTitle={sectionTitle} />}
      styles={{ body: { background: 'var(--color-neutral-25)' } }}
      destroyOnHidden
    >
      <div className="mb-4 flex flex-wrap items-center gap-x-4 gap-y-1 rounded-lg border border-neutral-100 bg-neutral-0 px-4 py-2 text-small text-neutral-700">
        <span className="inline-flex items-center gap-1.5">
          <span className={`size-2 rounded-full ${peers.length ? 'bg-presence-online' : 'bg-presence-offline'}`} />
          {peers.length ? `${peers.length} người đang mở` : 'Không ai đang mở'}
        </span>
        {editing.length > 0 && (
          <span>
            Đang soạn: {editing.map((p) => `${p.userName} (${sectionTitle(p.section) ?? 'mục khác'})`).join(', ')}
          </span>
        )}
        {lastChange && <span>Cập nhật {formatRelative(lastChange)}</span>}
        <span>{words} từ</span>
        <span className="text-neutral-500">Bôi đen đoạn văn để nhận xét · không sửa được nội dung của nhóm</span>
      </div>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="min-w-0">
          {doc.failed ? (
            <Result
              status="error"
              title="Không mở được tài liệu của nhóm"
              extra={<Button onClick={doc.retry}>Thử lại</Button>}
            />
          ) : doc.sync ? (
            <PaperEditor doc={doc.sync.doc} editable={false} onChange={(j) => setJson(j as DocNode)} comments={layer} />
          ) : (
            <Skeleton active paragraph={{ rows: 12 }} />
          )}
        </div>
        <aside className="flex min-w-0 flex-col gap-2 lg:sticky lg:top-0 lg:max-h-[calc(100vh-8rem)] lg:overflow-y-auto">
          <h3 className="m-0 text-h3 font-semibold text-neutral-900">Nhận xét trực tiếp ({live.comments.length})</h3>
          <LiveCommentList
            comments={live.comments}
            staleIds={selection.staleIds}
            activeId={selection.activeId}
            loading={live.loading}
            error={live.error}
            onRetry={live.retry}
            onSelect={selection.selectFromList}
            sectionTitle={sectionTitle}
            canManage={(c) => c.authorId === me}
            onEdit={(c) =>
              setForm({
                mode: 'edit',
                comment: c,
                initial: { kind: c.kind, comment: c.comment, suggestion: c.suggestion ?? '' }
              })
            }
            onDelete={remove}
            onToggleResolved={toggleResolved}
            emptyText="Chưa có nhận xét. Bôi đen một đoạn trong bài để bắt đầu."
          />
        </aside>
      </div>

      <LiveCommentFormModal
        open={form !== null}
        quote={form?.mode === 'create' ? form.selection.quote : (form?.comment.quote ?? '')}
        initial={form?.initial ?? INITIAL_BY_ACTION.comment}
        editing={form?.mode === 'edit'}
        onSubmit={submit}
        onClose={() => setForm(null)}
      />
    </Drawer>
  );
}
