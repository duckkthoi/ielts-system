import { Extension, Node, mergeAttributes } from '@tiptap/react';
import type { Node as PmNode } from '@tiptap/pm/model';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { ySyncPluginKey } from '@tiptap/y-tiptap';

// ─── Khung mục của paper trong trình soạn (BR-29) ─────────────────────────────
// Tài liệu = dãy `paperSection` theo khung giáo viên đặt; tiêu đề mục chỉ đọc. Sinh viên viết tự do bên trong mục
// nhưng không xoá / gộp / đổi thứ tự mục được: plugin chặn mọi thay đổi làm đổi danh sách mục, trừ thay đổi đến từ
// đồng bộ Yjs (khởi tạo khung, update của thành viên khác).

/** Tài liệu chỉ gồm các mục. */
export const PaperDocument = Node.create({
  name: 'doc',
  topNode: true,
  content: 'paperSection+'
});

export const PaperSection = Node.create({
  name: 'paperSection',
  content: 'block+',
  defining: true,
  isolating: true,

  addAttributes() {
    return {
      sectionKey: { default: '' },
      title: { default: '' },
      required: { default: true }
    };
  },

  parseHTML() {
    return [{ tag: 'section[data-section-key]' }];
  },

  renderHTML({ node, HTMLAttributes }) {
    return [
      'section',
      mergeAttributes(HTMLAttributes, {
        'data-section-key': node.attrs.sectionKey,
        id: `muc-${node.attrs.sectionKey}`,
        class: 'paper-section'
      }),
      ['h2', { contenteditable: 'false', class: 'paper-section-title' }, String(node.attrs.title)],
      ['div', { class: 'paper-section-body' }, 0]
    ];
  }
});

function sectionKeys(doc: PmNode): string {
  const keys: string[] = [];
  doc.forEach((child) => {
    if (child.type.name === 'paperSection') keys.push(String(child.attrs.sectionKey));
  });
  return keys.join('|');
}

/** Chặn thay đổi làm mất / thêm / đổi thứ tự mục khi người dùng gõ, xoá, dán. */
export const SectionGuard = Extension.create({
  name: 'paperSectionGuard',
  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: new PluginKey('paperSectionGuard'),
        filterTransaction(tr, state) {
          if (!tr.docChanged || tr.getMeta(ySyncPluginKey)) return true;
          return sectionKeys(tr.doc) === sectionKeys(state.doc);
        }
      })
    ];
  }
});
