'use client';

import { useCallback, useMemo, useState } from 'react';

import type { PaperCommentLayer } from '@/components/PaperEditor';
import type { LiveComment } from '@/lib/types/paper';

// ─── Nối danh sách nhận xét với lớp tô trên trình soạn (H4) ───────────────────
// Bấm đoạn đã tô → chọn thẻ + cuộn thẻ vào tầm nhìn; bấm thẻ → cuộn tới đoạn trên tài liệu. Ghi lại các nhận xét có
// đoạn văn đã đổi (lớp tô báo lên) để thẻ hiện "Đoạn văn đã thay đổi".

const scrollTo = (el: Element | null) => el?.scrollIntoView({ behavior: 'smooth', block: 'center' });

export function useCommentSelection(comments: LiveComment[]) {
  const [activeId, setActiveId] = useState<number | null>(null);
  const [staleIds, setStaleIds] = useState<number[]>([]);

  const marks = useMemo(
    () =>
      comments.map((c) => ({
        id: c.id,
        startOffset: c.startOffset,
        endOffset: c.endOffset,
        quote: c.quote,
        resolved: !!c.resolvedAt
      })),
    [comments]
  );

  const selectFromDoc = useCallback((id: number) => {
    setActiveId(id);
    scrollTo(document.getElementById(`live-comment-${id}`));
  }, []);

  const selectFromList = useCallback((id: number) => {
    setActiveId(id);
    scrollTo(document.querySelector(`[data-live="${id}"]`));
  }, []);

  const layer: PaperCommentLayer = useMemo(
    () => ({ marks, activeId, onMarkClick: selectFromDoc, onStaleChange: setStaleIds }),
    [marks, activeId, selectFromDoc]
  );

  return { activeId, staleIds, layer, selectFromList };
}
