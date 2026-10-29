import type { Node as PmNode } from '@tiptap/pm/model';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { Decoration, DecorationSet } from '@tiptap/pm/view';
import { mergeAttributes, Node } from '@tiptap/react';

import { parseRefIds } from '@/lib/utils/paperDoc';

// ─── Trích dẫn trong bài (docs/12 mục 5.3, BR-30) ─────────────────────────────
// Node `citation` chỉ lưu id tài liệu trong thư viện nhóm (`refIds` = "3,7" — thuộc tính Yjs nên dùng chuỗi), không lưu
// chữ: nhãn "[1]" / "(LeCun et al., 2015)" phụ thuộc thứ tự xuất hiện trong cả bài và thư viện, nên được tính lại mỗi lần
// đổi và gắn bằng decoration `data-label` (CSS hiện bằng `content: attr(data-label)`).

export interface CitationStorage {
  /** Nhãn của từng chỗ trích theo thứ tự xuất hiện — do màn hình đặt (cần thư viện + kiểu trích dẫn). */
  labeler: (sequence: number[][]) => string[];
  /** Bấm vào một trích dẫn (để sửa); `null` = chỉ đọc. */
  onClick: ((pos: number, ids: number[]) => void) | null;
}

declare module '@tiptap/core' {
  interface Storage {
    citation: CitationStorage;
  }
  interface Commands<ReturnType> {
    citation: {
      /** Đặt cách tính nhãn + xử lý bấm (thư viện / kiểu trích dẫn đổi) rồi vẽ lại nhãn. */
      setCitationContext: (context: CitationStorage) => ReturnType;
    };
  }
}

/** Meta của transaction chỉ để vẽ lại nhãn (thư viện / kiểu trích dẫn vừa đổi, tài liệu không đổi). */
export const CITATION_REFRESH = 'citationRefresh';

const key = new PluginKey('citationLabels');

export const Citation = Node.create<Record<string, never>, CitationStorage>({
  name: 'citation',
  group: 'inline',
  inline: true,
  atom: true,
  selectable: true,

  addStorage() {
    return { labeler: (sequence) => sequence.map(() => '[?]'), onClick: null };
  },

  addCommands() {
    return {
      setCitationContext:
        (context) =>
        ({ tr, dispatch }) => {
          if (dispatch) {
            this.storage.labeler = context.labeler;
            this.storage.onClick = context.onClick;
            tr.setMeta(CITATION_REFRESH, true);
          }
          return true;
        }
    };
  },

  addAttributes() {
    return {
      refIds: {
        default: '',
        parseHTML: (el) => el.getAttribute('data-ref-ids') ?? '',
        renderHTML: (attrs) => ({ 'data-ref-ids': attrs.refIds })
      }
    };
  },

  parseHTML() {
    return [{ tag: 'span[data-citation]' }];
  },

  renderHTML({ HTMLAttributes }) {
    return ['span', mergeAttributes(HTMLAttributes, { 'data-citation': '', class: 'paper-citation' })];
  },

  addProseMirrorPlugins() {
    const storage = this.storage;
    let cacheKey = '';
    let cacheLabels: string[] = [];
    const labelsFor = (sequence: number[][], refresh: boolean) => {
      const k = JSON.stringify(sequence);
      if (refresh || k !== cacheKey) {
        cacheKey = k;
        cacheLabels = storage.labeler(sequence);
      }
      return cacheLabels;
    };
    let pendingRefresh = false;

    return [
      new Plugin({
        key,
        state: {
          init: () => 0,
          // Đếm lần "vẽ lại": đổi số này mới tính lại nhãn dù chuỗi trích dẫn không đổi.
          apply: (tr, v) => {
            if (tr.getMeta(CITATION_REFRESH)) {
              pendingRefresh = true;
              return v + 1;
            }
            return v;
          }
        },
        props: {
          decorations(state) {
            const found: { pos: number; node: PmNode; ids: number[] }[] = [];
            state.doc.descendants((node, pos) => {
              if (node.type.name === 'citation') found.push({ pos, node, ids: parseRefIds(node.attrs.refIds) });
            });
            if (found.length === 0) return DecorationSet.empty;
            const labels = labelsFor(
              found.map((f) => f.ids),
              pendingRefresh
            );
            pendingRefresh = false;
            return DecorationSet.create(
              state.doc,
              found.map((f, i) =>
                Decoration.node(f.pos, f.pos + f.node.nodeSize, {
                  'data-label': labels[i] || '[?]',
                  'aria-label': `Trích dẫn ${labels[i] ?? ''}`
                })
              )
            );
          },
          handleClickOn(_view, _pos, node, nodePos) {
            if (node.type.name !== 'citation' || !storage.onClick) return false;
            storage.onClick(nodePos, parseRefIds(node.attrs.refIds));
            return true;
          }
        }
      })
    ];
  }
});
