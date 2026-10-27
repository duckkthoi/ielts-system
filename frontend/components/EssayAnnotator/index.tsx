'use client';

import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';

import { Tag } from 'antd';
import clsx from 'clsx';
import { MessageSquare } from 'lucide-react';

import { ALL_LAYERS, buildSegments, effectiveEdit, layoutMargin, mergeBars, trimRange } from '@/lib/utils/annotations';
import type { Layers, Segment } from '@/lib/utils/annotations';

import { AnnotationCard } from './AnnotationCard';
import { BUBBLE_HEIGHT, CARD_HEIGHT, LAYER_LABEL, TOOLBAR_ACTIONS, WAVY_CLASS } from './const';
import type { AnnotateAction, AnnotationView } from './type';

export { studentAnnotationViews } from './studentViews';
export type { AnnotateAction, AnnotationView } from './type';

// ─── Bài viết kèm đánh dấu kiểu "Word đã chấm" (F5b) ──────────────────────────
// Tách bài thành đoạn theo offset (lib/utils/annotations.ts) rồi vẽ bằng span — KHÔNG dùng innerHTML, chữ gốc
// của sinh viên giữ nguyên, sửa (thay / xoá / thêm chữ) chỉ là lớp hiển thị. Lề trái: thanh dọc ở các dòng có
// đánh dấu. Lề phải: bong bóng nhận xét canh theo dòng, tự dàn không chồng nhau; chọn một đánh dấu → thẻ chi tiết.
// Giáo viên bôi đen → thanh công cụ; sinh viên (readOnly) chỉ xem.

interface Props {
  content: string;
  annotations: AnnotationView[];
  readOnly?: boolean;
  selectedKey: string | null;
  onSelect: (key: string | null) => void;
  /** Tăng số này để cuộn tới đánh dấu đang chọn (vd bấm ở danh sách bên phải). */
  scrollNonce?: number;
  onAction?: (action: AnnotateAction, range: { start: number; end: number }) => void;
  onEdit?: (key: string) => void;
  onDelete?: (key: string) => void;
}

/** Vị trí trong DOM (nút + offset) → offset trong bài; null nếu rơi vào chữ thêm (không phải chữ gốc). */
function pointToOffset(node: Node, offset: number, length: number): number | null {
  const el = node.nodeType === Node.TEXT_NODE ? node.parentElement : (node as Element);
  if (!el || el.closest('[data-virtual]')) return null;
  const seg = el.closest<HTMLElement>('[data-start]');
  if (seg) {
    const start = Number(seg.dataset.start);
    if (node.nodeType === Node.TEXT_NODE) return start + offset;
    return offset === 0 ? start : Number(seg.dataset.end);
  }
  // Điểm nằm trên chính khung bài (giữa các đoạn): lấy mốc đầu của đoạn kế tiếp.
  const next = (node as Element).childNodes[offset] as HTMLElement | undefined;
  return next?.dataset?.start !== undefined ? Number(next.dataset.start) : length;
}

function SegmentView({
  seg,
  anns,
  layers,
  active,
  onPick,
  onHover
}: {
  seg: Segment;
  anns: AnnotationView[];
  layers: Layers;
  active: boolean;
  onPick: () => void;
  onHover: (on: boolean) => void;
}) {
  if (anns.length === 0) {
    return (
      <span data-start={seg.start} data-end={seg.end}>
        {seg.text}
      </span>
    );
  }
  const bg = active
    ? 'bg-annot-comment-active'
    : anns.some((a) => a.kind === 'HIGHLIGHT')
      ? 'bg-highlight-comment'
      : anns.some((a) => a.kind === 'GOOD')
        ? 'bg-annot-good'
        : anns.some((a) => a.kind === 'COMMENT' || (layers.comment && a.comment))
          ? 'bg-annot-comment'
          : '';
  const err = layers.error ? anns.find((a) => a.kind === 'ERROR' && a.errorCode && a.category) : undefined;
  const strike =
    layers.edit &&
    anns.some((a) => {
      const e = effectiveEdit(a);
      return e === 'REPLACE' || e === 'DELETE';
    });
  return (
    <>
      <span
        data-start={seg.start}
        data-end={seg.end}
        data-anns={seg.keys.join(' ')}
        data-first={seg.firstOf.join(' ') || undefined}
        className={clsx('cursor-pointer rounded-sm transition-colors', bg)}
        onClick={onPick}
        onMouseEnter={() => onHover(true)}
        onMouseLeave={() => onHover(false)}
      >
        <span className={err?.category ? WAVY_CLASS[err.category] : undefined}>
          <span className={strike ? 'text-annot-edit line-through decoration-annot-edit' : undefined}>{seg.text}</span>
        </span>
      </span>
      {seg.inserts.map((ins) => (
        <span
          key={ins.key}
          data-virtual
          contentEditable={false}
          className="ml-1 text-annot-edit underline decoration-annot-edit select-none"
        >
          {ins.text}
        </span>
      ))}
    </>
  );
}

export function EssayAnnotator({
  content,
  annotations,
  readOnly = false,
  selectedKey,
  onSelect,
  scrollNonce = 0,
  onAction,
  onEdit,
  onDelete
}: Props) {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const articleRef = useRef<HTMLDivElement>(null);
  const cardRef = useRef<HTMLDivElement>(null);
  const [layers, setLayers] = useState<Layers>(ALL_LAYERS);
  const [hovered, setHovered] = useState<string | null>(null);
  const [toolbar, setToolbar] = useState<{ left: number; top: number; start: number; end: number } | null>(null);
  const [layout, setLayout] = useState<{ bars: { top: number; bottom: number }[]; tops: Record<string, number> }>({
    bars: [],
    tops: {}
  });
  const [resizeTick, setResizeTick] = useState(0);
  const [cardHeight, setCardHeight] = useState(CARD_HEIGHT);

  const byKey = useMemo(() => Object.fromEntries(annotations.map((a) => [a.key, a])), [annotations]);
  const segments = useMemo(() => buildSegments(content, annotations, layers), [content, annotations, layers]);
  const visible = useMemo(() => new Set(segments.flatMap((s) => s.keys)), [segments]);
  const selected = selectedKey && visible.has(selectedKey) ? byKey[selectedKey] : null;
  // Lề phải: bong bóng cho đánh dấu có nhận xét; đánh dấu đang chọn hiện thẻ chi tiết.
  const marginKeys = useMemo(
    () =>
      annotations
        .filter((a) => visible.has(a.key) && ((layers.comment && a.comment) || a.key === selected?.key))
        .map((a) => a.key),
    [annotations, visible, layers.comment, selected]
  );

  // Đo lại khi khung đổi kích thước.
  useEffect(() => {
    const el = wrapperRef.current;
    if (!el) return;
    const ro = new ResizeObserver(() => setResizeTick((t) => t + 1));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  // Đo vị trí dòng của từng đánh dấu → thanh dọc lề trái + vị trí bong bóng lề phải (làm sau khi vẽ xong).
  useLayoutEffect(() => {
    const raf = requestAnimationFrame(() => {
      const wrap = wrapperRef.current;
      const art = articleRef.current;
      if (!wrap || !art) return;
      const base = wrap.getBoundingClientRect().top;
      const rects: { top: number; bottom: number }[] = [];
      art.querySelectorAll<HTMLElement>('[data-anns]').forEach((el) => {
        for (const r of el.getClientRects()) rects.push({ top: r.top - base, bottom: r.bottom - base });
      });
      const measuredCard = cardRef.current?.offsetHeight;
      if (measuredCard && measuredCard !== cardHeight) setCardHeight(measuredCard);
      const items = marginKeys.flatMap((key) => {
        const el = art.querySelector<HTMLElement>(`[data-first~="${CSS.escape(key)}"]`);
        if (!el) return [];
        const top = el.getBoundingClientRect().top - base;
        return [{ key, top, height: key === selected?.key ? (measuredCard ?? cardHeight) : BUBBLE_HEIGHT }];
      });
      const next = { bars: mergeBars(rects), tops: layoutMargin(items) };
      setLayout((prev) => (JSON.stringify(prev) === JSON.stringify(next) ? prev : next));
    });
    return () => cancelAnimationFrame(raf);
  }, [segments, marginKeys, selected, cardHeight, resizeTick]);

  // Bấm ở danh sách bên phải → cuộn tới đoạn trên bài.
  useEffect(() => {
    if (!scrollNonce || !selectedKey) return;
    articleRef.current
      ?.querySelector<HTMLElement>(`[data-first~="${CSS.escape(selectedKey)}"]`)
      ?.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }, [scrollNonce, selectedKey]);

  const handleMouseUp = () => {
    if (readOnly || !onAction) return;
    const sel = window.getSelection();
    const art = articleRef.current;
    const wrap = wrapperRef.current;
    if (!sel || sel.isCollapsed || sel.rangeCount === 0 || !art || !wrap) return;
    const range = sel.getRangeAt(0);
    if (!art.contains(range.commonAncestorContainer)) return;
    const s = pointToOffset(range.startContainer, range.startOffset, content.length);
    const e = pointToOffset(range.endContainer, range.endOffset, content.length);
    const trimmed = s === null || e === null ? null : trimRange(content, s, e);
    if (!trimmed) {
      setToolbar(null);
      return;
    }
    const rect = range.getBoundingClientRect();
    const wrapRect = wrap.getBoundingClientRect();
    setToolbar({
      left: Math.max(0, Math.min(rect.left - wrapRect.left, wrapRect.width - 420)),
      top: rect.bottom - wrapRect.top + 6,
      ...trimmed
    });
  };

  const pickSmallest = (keys: string[]) =>
    keys.reduce((best, k) => {
      const a = byKey[k];
      const b = byKey[best];
      return a.endOffset - a.startOffset < b.endOffset - b.startOffset ? k : best;
    }, keys[0]);

  const activeKey = hovered ?? selected?.key ?? null;

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-1 text-small text-neutral-500">
        Hiện:
        {(Object.keys(LAYER_LABEL) as (keyof Layers)[]).map((k) => (
          <Tag.CheckableTag key={k} checked={layers[k]} onChange={(v) => setLayers((l) => ({ ...l, [k]: v }))}>
            {LAYER_LABEL[k]}
          </Tag.CheckableTag>
        ))}
      </div>
      <div
        ref={wrapperRef}
        className="relative grid grid-cols-[10px_minmax(0,1fr)] gap-x-2 rounded-lg border border-neutral-100 bg-neutral-0 py-6 pr-3 pl-2 shadow-sm sm:grid-cols-[10px_minmax(0,1fr)_minmax(0,180px)] sm:py-8"
      >
        {/* Lề trái: thanh dọc ở các dòng có đánh dấu */}
        <div className="relative" aria-hidden>
          {layout.bars.map((b, i) => (
            <span
              key={i}
              className="absolute left-1 w-0.5 rounded-full bg-brand-500"
              style={{ top: b.top, height: b.bottom - b.top }}
            />
          ))}
        </div>

        <div
          ref={articleRef}
          className="px-2 font-serif text-[16px]/[28px] whitespace-pre-wrap text-neutral-900 sm:px-4 sm:text-essay"
          onMouseUp={handleMouseUp}
          onMouseDown={() => setToolbar(null)}
        >
          {segments.map((seg) => (
            <SegmentView
              key={seg.start}
              seg={seg}
              anns={seg.keys.map((k) => byKey[k])}
              layers={layers}
              active={activeKey !== null && seg.keys.includes(activeKey)}
              onPick={() => {
                if (window.getSelection()?.isCollapsed === false) return;
                onSelect(pickSmallest(seg.keys));
              }}
              onHover={(on) => setHovered(on ? pickSmallest(seg.keys) : null)}
            />
          ))}
        </div>

        {/* Lề phải: bong bóng nhận xét / thẻ chi tiết (ẩn trên màn hình hẹp, thẻ chi tiết hiện dưới bài) */}
        <div className="relative hidden sm:block">
          {marginKeys.map((key) => {
            const a = byKey[key];
            const top = layout.tops[key];
            if (top === undefined) return null;
            if (key === selected?.key) {
              return (
                <div key={key} ref={cardRef} className="absolute inset-x-0 z-10" style={{ top }}>
                  <AnnotationCard
                    ann={a}
                    readOnly={readOnly}
                    onClose={() => onSelect(null)}
                    onEdit={() => onEdit?.(key)}
                    onDelete={() => onDelete?.(key)}
                  />
                </div>
              );
            }
            return (
              <button
                key={key}
                type="button"
                onClick={() => onSelect(key)}
                onMouseEnter={() => setHovered(key)}
                onMouseLeave={() => setHovered(null)}
                className={clsx(
                  'absolute inset-x-0 flex h-7 cursor-pointer items-center gap-1.5 rounded-md border px-2 text-left text-caption transition-colors',
                  hovered === key
                    ? 'border-brand-500 bg-brand-50 text-brand-700 shadow-sm'
                    : 'border-neutral-100 bg-neutral-25 text-neutral-500'
                )}
                style={{ top }}
              >
                <MessageSquare size={12} className="shrink-0" />
                <span className="truncate">{a.comment}</span>
              </button>
            );
          })}
        </div>

        {toolbar && (
          <div
            className="absolute z-20 flex flex-wrap gap-0.5 rounded-lg border border-neutral-100 bg-neutral-0 p-1 shadow-lg"
            style={{ left: toolbar.left, top: toolbar.top }}
            onMouseDown={(e) => e.preventDefault()}
          >
            {TOOLBAR_ACTIONS.map((t) => (
              <button
                key={t.action}
                type="button"
                onClick={() => {
                  onAction?.(t.action, { start: toolbar.start, end: toolbar.end });
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

      {/* Màn hình hẹp: thẻ chi tiết hiện dưới bài */}
      {selected && (
        <div className="sm:hidden">
          <AnnotationCard
            ann={selected}
            readOnly={readOnly}
            onClose={() => onSelect(null)}
            onEdit={() => onEdit?.(selected.key)}
            onDelete={() => onDelete?.(selected.key)}
          />
        </div>
      )}
    </div>
  );
}
