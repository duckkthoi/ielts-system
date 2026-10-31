'use client';

import 'katex/dist/katex.min.css';
import './paperEditor.css';

import { useEffect, useMemo, useRef, useState } from 'react';

import { Alert, Skeleton } from 'antd';
import clsx from 'clsx';
import { Decoration, DecorationSet } from '@tiptap/pm/view';
import { EditorContent, useEditor, type JSONContent } from '@tiptap/react';

import { AnnotationCard, TOOLBAR_ACTIONS, WAVY_CLASS } from '@/components/EssayAnnotator';
import type { AnnotateAction, AnnotationView } from '@/components/EssayAnnotator';
import type { CitationStyle } from '@/lib/types/assignment';
import type { CslItem } from '@/lib/types/paper';
import { trimRange } from '@/lib/utils/annotations';
import { bibliographyEntries, citationLabels } from '@/lib/utils/citation';
import { citationSequence, docTextPositions, offsetsToRange, rangeToOffsets, type DocNode } from '@/lib/utils/paperDoc';

import { AnnotationDecorations, annotationKey } from './annotationDecorations';
import { paperViewerExtensions } from './extensions';
import { PaperBibliography } from './PaperBibliography';

// ─── Xem một lần nộp của bài PAPER (B25a) ─────────────────────────────────────
// Dựng từ JSON lưu lúc nộp + các tài liệu được trích lúc đó (không đọc thư viện hiện tại — sửa thư viện sau khi nộp không
// làm đổi bản nộp). Chỉ đọc; nhãn trích dẫn và References tính như lúc soạn.
// B25d: có `annotations` thì tô các đánh dấu (neo theo offset trên `content` — chữ thuần của lần nộp, dựng lại bằng
// docTextPositions); có `onAction` thì bôi đen để thêm. Chữ dựng lại lệch `content` (bản nộp cũ) → chỉ hiện danh sách.

/** Bài PAPER không có lớp "sửa chữ" (thay / xoá / thêm) — bản có định dạng không vẽ được chữ thay thế. */
const PAPER_ACTIONS = TOOLBAR_ACTIONS.filter((t) => !['replace', 'delete', 'insert'].includes(t.action));

function annotationClass(a: AnnotationView, active: boolean): string {
  if (active) return 'cursor-pointer rounded-sm bg-annot-comment-active';
  if (a.kind === 'ERROR') {
    return clsx('cursor-pointer rounded-sm', a.category ? WAVY_CLASS[a.category] : 'bg-annot-comment');
  }
  const bg = a.kind === 'HIGHLIGHT' ? 'bg-highlight-comment' : a.kind === 'GOOD' ? 'bg-annot-good' : 'bg-annot-comment';
  return clsx('cursor-pointer rounded-sm', bg);
}

interface AnnotationProps {
  /** Chữ thuần của lần nộp — offset của đánh dấu tính trên chuỗi này. */
  content: string;
  annotations: AnnotationView[];
  selectedKey: string | null;
  onSelect: (key: string | null) => void;
  /** Tăng số này để cuộn tới đánh dấu đang chọn. */
  scrollNonce?: number;
  /** Bỏ trống = chỉ xem. */
  onAction?: (action: AnnotateAction, range: { start: number; end: number }) => void;
  onEdit?: (key: string) => void;
  onDelete?: (key: string) => void;
}

export function PaperViewer({
  contentJson,
  references,
  style,
  footer,
  annotate
}: {
  contentJson: Record<string, unknown>;
  references: { id: number; csl: CslItem }[];
  style: CitationStyle;
  footer?: React.ReactNode;
  annotate?: AnnotationProps;
}) {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const [toolbar, setToolbar] = useState<{ left: number; top: number; start: number; end: number } | null>(null);
  // editorProps chỉ đọc một lần lúc tạo editor → giữ bản mới nhất của danh sách / callback trong ref.
  const live = useRef<{ annotations: AnnotationView[]; onSelect?: (key: string | null) => void }>({ annotations: [] });
  const ranges = useRef<{ key: string; from: number; to: number }[]>([]);

  const editor = useEditor(
    {
      extensions: [...paperViewerExtensions(), AnnotationDecorations],
      content: contentJson as JSONContent,
      editable: false,
      immediatelyRender: false,
      editorProps: {
        attributes: { class: 'paper-editor', lang: 'en', 'aria-label': 'Bài đã nộp' },
        handleClick: (_view, pos) => {
          const hits = ranges.current.filter((r) => r.from <= pos && pos < r.to);
          if (hits.length === 0 || window.getSelection()?.isCollapsed === false) return false;
          const smallest = hits.reduce((best, r) => (r.to - r.from < best.to - best.from ? r : best));
          live.current.onSelect?.(smallest.key);
          return true;
        }
      }
    },
    [contentJson]
  );

  const known = useMemo(() => new Map(references.map((r) => [r.id, r.csl])), [references]);
  const sequence = useMemo(() => citationSequence(contentJson as DocNode), [contentJson]);
  const bibliography = useMemo(() => bibliographyEntries(sequence, known, style), [sequence, known, style]);

  useEffect(() => {
    if (!editor || editor.isDestroyed) return;
    editor.commands.setCitationContext({ labeler: (seq) => citationLabels(seq, known, style), onClick: null });
  }, [editor, known, style]);

  // Bảng vị trí của bản có định dạng; chỉ dùng khi chữ dựng lại đúng bằng `content` đã lưu.
  const plain = annotate?.content;
  const mapping = useMemo(() => {
    if (!editor || plain === undefined) return null;
    const m = docTextPositions(editor.state.doc);
    return m.text === plain ? m.positions : null;
  }, [editor, plain]);
  const aligned = mapping !== null;

  const annotations = annotate?.annotations;
  const selectedKey = annotate?.selectedKey ?? null;
  useEffect(() => {
    live.current = { annotations: annotations ?? [], onSelect: annotate?.onSelect };
  });

  useEffect(() => {
    if (!editor || editor.isDestroyed) return;
    const decos: Decoration[] = [];
    const next: { key: string; from: number; to: number }[] = [];
    if (mapping && annotations) {
      for (const a of annotations) {
        const r = offsetsToRange(mapping, a.startOffset, a.endOffset);
        if (!r) continue;
        next.push({ key: a.key, ...r });
        decos.push(Decoration.inline(r.from, r.to, { class: annotationClass(a, a.key === selectedKey), 'data-ann': a.key }));
      }
    }
    ranges.current = next;
    editor.view.dispatch(editor.state.tr.setMeta(annotationKey, DecorationSet.create(editor.state.doc, decos)));
  }, [editor, mapping, annotations, selectedKey]);

  // Bấm ở danh sách bên phải → cuộn tới đánh dấu.
  const scrollNonce = annotate?.scrollNonce ?? 0;
  useEffect(() => {
    if (!scrollNonce || !selectedKey) return;
    wrapperRef.current
      ?.querySelector(`[data-ann="${CSS.escape(selectedKey)}"]`)
      ?.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }, [scrollNonce, selectedKey]);

  const handleMouseUp = () => {
    if (!annotate?.onAction || !mapping || !editor) return;
    const sel = window.getSelection();
    const wrap = wrapperRef.current;
    if (!sel || sel.isCollapsed || sel.rangeCount === 0 || !wrap) return;
    const range = sel.getRangeAt(0);
    if (!editor.view.dom.contains(range.commonAncestorContainer)) return;
    let offsets: { start: number; end: number } | null = null;
    try {
      const from = editor.view.posAtDOM(range.startContainer, range.startOffset);
      const to = editor.view.posAtDOM(range.endContainer, range.endOffset);
      const raw = rangeToOffsets(mapping, Math.min(from, to), Math.max(from, to));
      offsets = raw && trimRange(annotate.content, raw.start, raw.end);
    } catch {
      offsets = null;
    }
    if (!offsets) {
      setToolbar(null);
      return;
    }
    const rect = range.getBoundingClientRect();
    const wrapRect = wrap.getBoundingClientRect();
    setToolbar({
      left: Math.max(0, Math.min(rect.left - wrapRect.left, wrapRect.width - 320)),
      top: rect.bottom - wrapRect.top + 6,
      ...offsets
    });
  };

  const selected = annotate && selectedKey ? annotate.annotations.find((a) => a.key === selectedKey) : undefined;

  return (
    <div
      ref={wrapperRef}
      className="relative mx-auto flex w-full max-w-240 flex-col rounded-lg border border-neutral-100 bg-neutral-0 shadow-sm"
    >
      {annotate && editor && !aligned && annotate.annotations.length > 0 && (
        <Alert
          type="info"
          showIcon
          className="m-3"
          title="Không tô được đánh dấu lên bản có định dạng này — xem danh sách đánh dấu bên cạnh."
        />
      )}
      <div
        className="px-5 py-6 sm:px-8 sm:py-8"
        onMouseUp={handleMouseUp}
        onMouseDown={() => setToolbar(null)}
      >
        {editor ? <EditorContent editor={editor} /> : <Skeleton active paragraph={{ rows: 8 }} />}
        <PaperBibliography entries={bibliography} style={style} />
      </div>
      {selected && (
        <div className="border-t border-neutral-100 px-5 py-3 sm:px-8">
          <AnnotationCard
            ann={selected}
            readOnly={!annotate?.onAction}
            onClose={() => annotate?.onSelect(null)}
            onEdit={() => annotate?.onEdit?.(selected.key)}
            onDelete={() => annotate?.onDelete?.(selected.key)}
          />
        </div>
      )}
      {footer && (
        <div className="flex flex-wrap items-center justify-between gap-2 border-t border-neutral-100 px-5 py-2 sm:px-8">
          {footer}
        </div>
      )}
      {toolbar && annotate?.onAction && (
        <div
          className="absolute z-20 flex flex-wrap gap-0.5 rounded-lg border border-neutral-100 bg-neutral-0 p-1 shadow-lg"
          style={{ left: toolbar.left, top: toolbar.top }}
          onMouseDown={(e) => e.preventDefault()}
        >
          {PAPER_ACTIONS.map((t) => (
            <button
              key={t.action}
              type="button"
              onClick={() => {
                annotate.onAction?.(t.action, { start: toolbar.start, end: toolbar.end });
                setToolbar(null);
                window.getSelection()?.removeAllRanges();
              }}
              className={clsx(
                'inline-flex cursor-pointer items-center gap-1 rounded-md border-0 bg-transparent px-2 py-1 text-small hover:bg-neutral-50',
                t.tone ?? 'text-neutral-700'
              )}
            >
              {t.icon}
              {t.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
