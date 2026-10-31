import { Extension } from '@tiptap/react';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { DecorationSet } from '@tiptap/pm/view';

// ─── Tô các đánh dấu của giáo viên lên bản nộp Paper (B25d) ───────────────────
// Chỉ là lớp trang trí ProseMirror (không đổi tài liệu). PaperViewer tính sẵn DecorationSet rồi gửi bằng
// `tr.setMeta(annotationKey, set)`; tài liệu chỉ đọc nên không cần map vị trí khi sửa.

export const annotationKey = new PluginKey<DecorationSet>('paperAnnotations');

export const AnnotationDecorations = Extension.create({
  name: 'paperAnnotations',

  addProseMirrorPlugins() {
    return [
      new Plugin<DecorationSet>({
        key: annotationKey,
        state: {
          init: () => DecorationSet.empty,
          apply: (tr, old) => tr.getMeta(annotationKey) ?? old.map(tr.mapping, tr.doc)
        },
        props: {
          decorations: (state) => annotationKey.getState(state)
        }
      })
    ];
  }
});
