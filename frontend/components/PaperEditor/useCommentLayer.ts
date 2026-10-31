'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

import { Decoration, DecorationSet } from '@tiptap/pm/view';
import type { Editor } from '@tiptap/react';

import { trimRange } from '@/lib/utils/annotations';
import { isAnchored } from '@/lib/utils/liveComments';
import { docTextPositions, offsetsToRange, rangeToOffsets } from '@/lib/utils/paperDoc';

import { annotationKey } from './annotationDecorations';

// ─── Lớp nhận xét trực tiếp trên trình soạn Paper đang viết (H4) ──────────────
// Chỉ là decoration (không đổi tài liệu Yjs). Tính lại sau mỗi thay đổi của tài liệu (của mình hoặc người khác):
// nhận xét có đoạn tại offset còn đúng `quote` thì tô, lệch thì báo ra ngoài là "đã thay đổi" (không dò chỗ khác).
// Bôi đen (khi có `actions`) → offset trên chữ thuần + quote + mục chứa đoạn, hiện thanh công cụ để tạo nhận xét.

export interface CommentMark {
  id: number;
  startOffset: number;
  endOffset: number;
  quote: string;
  resolved: boolean;
}

export interface TextSelection {
  start: number;
  end: number;
  quote: string;
  sectionKey: string | null;
  /** Toạ độ so với khung trình soạn, để đặt thanh công cụ. */
  left: number;
  top: number;
}

export interface SelectionAction {
  key: string;
  label: string;
  icon?: React.ReactNode;
}

export interface PaperCommentLayer {
  marks: CommentMark[];
  activeId: number | null;
  onMarkClick?: (id: number) => void;
  /** Báo danh sách id không còn khớp đoạn văn (đã thay đổi). */
  onStaleChange?: (ids: number[]) => void;
  /** Có thì bôi đen hiện thanh công cụ với các nút này (+ "Bỏ chọn"); bấm nút → `onAction`. */
  actions?: SelectionAction[];
  onAction?: (key: string, sel: TextSelection) => void;
}

function markClass(m: CommentMark, active: boolean): string {
  if (active) return 'cursor-pointer rounded-sm bg-highlight-comment-active';
  return m.resolved ? 'cursor-pointer rounded-sm bg-annot-comment' : 'cursor-pointer rounded-sm bg-highlight-comment';
}

export function useCommentLayer(editor: Editor | null, layer: PaperCommentLayer | undefined, version: number) {
  const ranges = useRef<{ id: number; from: number; to: number }[]>([]);
  const lastStale = useRef('');
  const [selection, setSelection] = useState<TextSelection | null>(null);
  const layerRef = useRef(layer);
  useEffect(() => {
    layerRef.current = layer;
  });

  const marks = layer?.marks;
  const activeId = layer?.activeId ?? null;

  // Tô lại sau mỗi thay đổi tài liệu (`version`) hoặc danh sách nhận xét.
  useEffect(() => {
    if (!editor || editor.isDestroyed || !marks) return;
    const { text, positions } = docTextPositions(editor.state.doc);
    const decos: Decoration[] = [];
    const next: { id: number; from: number; to: number }[] = [];
    const lost: number[] = [];
    for (const m of marks) {
      const r = isAnchored(text, m) ? offsetsToRange(positions, m.startOffset, m.endOffset) : null;
      if (!r) {
        lost.push(m.id);
        continue;
      }
      next.push({ id: m.id, ...r });
      decos.push(
        Decoration.inline(r.from, r.to, { class: markClass(m, m.id === activeId), 'data-live': String(m.id) })
      );
    }
    ranges.current = next;
    editor.view.dispatch(editor.state.tr.setMeta(annotationKey, DecorationSet.create(editor.state.doc, decos)));
    if (lost.join() !== lastStale.current) {
      lastStale.current = lost.join();
      layerRef.current?.onStaleChange?.(lost);
    }
  }, [editor, marks, activeId, version]);

  /** Bấm vào đoạn đã tô → chọn nhận xét nhỏ nhất chứa vị trí đó. */
  const handleClick = useCallback((pos: number) => {
    const hits = ranges.current.filter((r) => r.from <= pos && pos < r.to);
    if (hits.length === 0 || window.getSelection()?.isCollapsed === false) return false;
    const smallest = hits.reduce((best, r) => (r.to - r.from < best.to - best.from ? r : best));
    layerRef.current?.onMarkClick?.(smallest.id);
    return true;
  }, []);

  /** Bôi đen xong (mouseup) trong khung `wrapper` → giữ vùng chọn để hiện thanh công cụ. */
  const handleMouseUp = useCallback(
    (wrapper: HTMLElement | null) => {
      if (!layerRef.current?.actions?.length || !editor || !wrapper) return;
      const onSelect = setSelection;
      const sel = window.getSelection();
      if (!sel || sel.isCollapsed || sel.rangeCount === 0) return onSelect(null);
      const range = sel.getRangeAt(0);
      if (!editor.view.dom.contains(range.commonAncestorContainer)) return;
      try {
        const from = editor.view.posAtDOM(range.startContainer, range.startOffset);
        const to = editor.view.posAtDOM(range.endContainer, range.endOffset);
        const { text, positions } = docTextPositions(editor.state.doc);
        const raw = rangeToOffsets(positions, Math.min(from, to), Math.max(from, to));
        const offsets = raw && trimRange(text, raw.start, raw.end);
        if (!offsets) return onSelect(null);
        const $from = editor.state.doc.resolve(Math.min(from, to));
        let sectionKey: string | null = null;
        for (let d = $from.depth; d > 0; d--) {
          if ($from.node(d).type.name === 'paperSection') {
            sectionKey = String($from.node(d).attrs.sectionKey);
            break;
          }
        }
        const rect = range.getBoundingClientRect();
        const box = wrapper.getBoundingClientRect();
        onSelect({
          ...offsets,
          quote: text.slice(offsets.start, offsets.end),
          sectionKey,
          left: Math.max(0, Math.min(rect.left - box.left, box.width - 360)),
          top: rect.bottom - box.top + 6
        });
      } catch {
        onSelect(null);
      }
    },
    [editor]
  );

  const clearSelection = useCallback(() => {
    setSelection(null);
    window.getSelection()?.removeAllRanges();
  }, []);

  return { handleClick, handleMouseUp, selection, clearSelection };
}
