'use client';

import 'katex/dist/katex.min.css';
import './paperEditor.css';

import { useEffect, useMemo } from 'react';

import { Skeleton } from 'antd';
import { EditorContent, useEditor, type JSONContent } from '@tiptap/react';

import type { CitationStyle } from '@/lib/types/assignment';
import type { CslItem } from '@/lib/types/paper';
import { bibliographyEntries, citationLabels } from '@/lib/utils/citation';
import { citationSequence, type DocNode } from '@/lib/utils/paperDoc';

import { paperViewerExtensions } from './extensions';
import { PaperBibliography } from './PaperBibliography';

// ─── Xem một lần nộp của bài PAPER (B25a) ─────────────────────────────────────
// Dựng từ JSON lưu lúc nộp + các tài liệu được trích lúc đó (không đọc thư viện hiện tại — sửa thư viện sau khi nộp không
// làm đổi bản nộp). Chỉ đọc; nhãn trích dẫn và References tính như lúc soạn.

export function PaperViewer({
  contentJson,
  references,
  style,
  footer
}: {
  contentJson: Record<string, unknown>;
  references: { id: number; csl: CslItem }[];
  style: CitationStyle;
  footer?: React.ReactNode;
}) {
  const editor = useEditor(
    {
      extensions: paperViewerExtensions(),
      content: contentJson as JSONContent,
      editable: false,
      immediatelyRender: false,
      editorProps: { attributes: { class: 'paper-editor', lang: 'en', 'aria-label': 'Bài đã nộp' } }
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

  return (
    <div className="mx-auto flex w-full max-w-240 flex-col rounded-lg border border-neutral-100 bg-neutral-0 shadow-sm">
      <div className="px-5 py-6 sm:px-8 sm:py-8">
        {editor ? <EditorContent editor={editor} /> : <Skeleton active paragraph={{ rows: 8 }} />}
        <PaperBibliography entries={bibliography} style={style} />
      </div>
      {footer && (
        <div className="flex flex-wrap items-center justify-between gap-2 border-t border-neutral-100 px-5 py-2 sm:px-8">
          {footer}
        </div>
      )}
    </div>
  );
}
