'use client';

import 'katex/dist/katex.min.css';
import './paperEditor.css';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { Skeleton } from 'antd';
import { EditorContent, useEditor, type JSONContent } from '@tiptap/react';
import type * as Y from 'yjs';

import type { CitationStyle } from '@/lib/types/assignment';
import type { CslItem, ReferenceDto } from '@/lib/types/paper';
import { bibliographyEntries, citationLabels } from '@/lib/utils/citation';
import { citationSequence, type DocNode } from '@/lib/utils/paperDoc';

import { CitationPicker, type PickerTarget } from './CitationPicker';
import { paperExtensions, type MathClick } from './extensions';
import { MathModal, type MathTarget } from './MathModal';
import { PaperBibliography } from './PaperBibliography';
import { PaperToolbar } from './PaperToolbar';

// ─── Trình soạn paper (docs/07 mục 7.2, docs/12 mục 5.1) ──────────────────────
// Nội dung nằm trong Y.Doc do `PaperSync` đồng bộ với server; trình soạn chỉ hiển thị và sửa nó (Collaboration).
// Tờ giấy trắng rộng tối đa 960, thanh công cụ dính trên cùng khi cuộn. `editable = false`: chỉ đọc, ẩn thanh công cụ.
// Có `citations` (thư viện nhóm + kiểu trích dẫn): chèn / sửa trích dẫn, nhãn tự đánh lại, danh mục cuối bài (BR-30).

export interface PaperCitations {
  style: CitationStyle;
  refs: ReferenceDto[];
  /** Mở thư viện để thêm tài liệu. */
  onOpenLibrary: () => void;
  /** Tải lại thư viện (thành viên khác có thể vừa thêm) — gọi khi mở hộp chọn. */
  onReload: () => void;
}

export function PaperEditor({
  doc,
  editable,
  footer,
  citations,
  onChange
}: {
  doc: Y.Doc;
  editable: boolean;
  footer?: React.ReactNode;
  citations?: PaperCitations;
  /** Gọi sau mỗi thay đổi (của mình hoặc của thành viên khác) với JSON của tài liệu. */
  onChange?: (json: JSONContent) => void;
}) {
  const [math, setMath] = useState<MathTarget | null>(null);
  const [picker, setPicker] = useState<PickerTarget | null>(null);
  const [sequence, setSequence] = useState<number[][]>([]);
  const onChangeRef = useRef(onChange);
  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  // Bấm vào công thức → mở hộp sửa; chỉ đọc thì hộp không hiện (xem `target` của MathModal bên dưới).
  const onMathClick = useCallback<MathClick>((kind, node, pos) => {
    setMath({ kind, latex: String(node.attrs.latex ?? ''), pos });
  }, []);

  const editor = useEditor(
    {
      extensions: paperExtensions(doc, onMathClick),
      editable,
      immediatelyRender: false,
      shouldRerenderOnTransaction: false,
      editorProps: { attributes: { class: 'paper-editor', lang: 'en', 'aria-label': 'Nội dung paper' } },
      onCreate: ({ editor: e }) => handleChange(e.getJSON()),
      onUpdate: ({ editor: e }) => handleChange(e.getJSON())
    },
    [doc]
  );

  function handleChange(json: JSONContent) {
    onChangeRef.current?.(json);
    const next = citationSequence(json as DocNode);
    // Chỉ đổi state khi chuỗi trích dẫn đổi — gõ chữ thường không làm vẽ lại danh mục.
    setSequence((cur) => (JSON.stringify(cur) === JSON.stringify(next) ? cur : next));
  }

  const style = citations?.style ?? 'IEEE';
  const known = useMemo(
    () => new Map<number, CslItem>((citations?.refs ?? []).map((r) => [r.id, r.csl])),
    [citations?.refs]
  );
  const bibliography = useMemo(() => bibliographyEntries(sequence, known, style), [sequence, known, style]);

  // Thư viện / kiểu trích dẫn đổi → đặt lại cách tính nhãn (lệnh của citationNode tự vẽ lại nhãn).
  useEffect(() => {
    if (!editor || editor.isDestroyed) return;
    editor.commands.setCitationContext({
      labeler: (seq) => citationLabels(seq, known, style),
      onClick: editable && citations ? (pos, ids) => setPicker({ pos, ids }) : null
    });
  }, [editor, known, style, editable, citations]);

  const openPicker = () => {
    citations?.onReload();
    setPicker({ ids: [] });
  };

  const saveCitation = (ids: number[]) => {
    if (!editor || !picker) return;
    const refIds = ids.join(',');
    if (picker.pos === undefined) {
      editor.chain().focus().insertContent({ type: 'citation', attrs: { refIds } }).run();
    } else {
      const pos = picker.pos;
      editor
        .chain()
        .focus()
        .command(({ tr }) => {
          tr.setNodeMarkup(pos, undefined, { refIds });
          return true;
        })
        .run();
    }
    setPicker(null);
  };

  const removeCitation = () => {
    if (!editor || picker?.pos === undefined) return;
    editor
      .chain()
      .focus()
      .deleteRange({ from: picker.pos, to: picker.pos + 1 })
      .run();
    setPicker(null);
  };

  useEffect(() => {
    if (editor && editor.isEditable !== editable) editor.setEditable(editable);
  }, [editor, editable]);

  const saveMath = (latex: string) => {
    if (!editor || !math) return;
    const c = editor.chain().focus();
    if (math.pos === undefined) {
      (math.kind === 'block' ? c.insertBlockMath({ latex }) : c.insertInlineMath({ latex })).run();
    } else if (math.kind === 'block') {
      c.updateBlockMath({ latex, pos: math.pos }).run();
    } else {
      c.updateInlineMath({ latex, pos: math.pos }).run();
    }
    setMath(null);
  };

  const deleteMath = () => {
    if (!editor || math?.pos === undefined) return;
    const c = editor.chain().focus();
    (math.kind === 'block' ? c.deleteBlockMath({ pos: math.pos }) : c.deleteInlineMath({ pos: math.pos })).run();
    setMath(null);
  };

  return (
    <div className="mx-auto flex w-full max-w-240 flex-col rounded-lg border border-neutral-100 bg-neutral-0 shadow-sm">
      {editor && editable && (
        <div className="sticky top-16 z-10 rounded-t-lg bg-neutral-0">
          <PaperToolbar editor={editor} onMath={setMath} onCite={citations ? openPicker : undefined} />
        </div>
      )}
      <div className="px-5 py-6 sm:px-8 sm:py-8">
        {editor ? <EditorContent editor={editor} /> : <Skeleton active paragraph={{ rows: 8 }} />}
        {citations && <PaperBibliography entries={bibliography} style={style} />}
      </div>
      {footer && (
        <div className="flex flex-wrap items-center justify-between gap-2 border-t border-neutral-100 px-5 py-2 sm:px-8">
          {footer}
        </div>
      )}
      <MathModal
        target={editable ? math : null}
        onSave={saveMath}
        onDelete={deleteMath}
        onClose={() => setMath(null)}
      />
      {citations && (
        <CitationPicker
          target={editable ? picker : null}
          refs={citations.refs}
          style={style}
          onSave={saveCitation}
          onRemove={removeCitation}
          onOpenLibrary={() => {
            setPicker(null);
            citations.onOpenLibrary();
          }}
          onClose={() => setPicker(null)}
        />
      )}
    </div>
  );
}
