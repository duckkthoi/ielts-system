import { CodeBlockLowlight } from '@tiptap/extension-code-block-lowlight';
import { Collaboration } from '@tiptap/extension-collaboration';
import { Image } from '@tiptap/extension-image';
import { Mathematics } from '@tiptap/extension-mathematics';
import { TableKit } from '@tiptap/extension-table';
import { Placeholder } from '@tiptap/extensions';
import type { Node as PmNode } from '@tiptap/pm/model';
import { getSchema, type AnyExtension } from '@tiptap/react';
import { StarterKit } from '@tiptap/starter-kit';
import { common, createLowlight } from 'lowlight';
import type * as Y from 'yjs';

import { Citation } from './citationNode';
import { PaperDocument, PaperSection, SectionGuard } from './sectionNode';

// ─── Bộ extension của trình soạn paper (docs/12 mục 5.1, FR-72) ───────────────
// Tài liệu = các mục cố định; trong mục: tiêu đề cấp 3–4, đậm / nghiêng / gạch chân, danh sách, trích dẫn khối,
// bảng, hình, khối code tô màu (lowlight), công thức LaTeX (KaTeX), trích dẫn tới thư viện nhóm (citationNode).
// Lịch sử hoàn tác do Yjs giữ (Collaboration) nên tắt `undoRedo` của StarterKit; tắt `trailingNode` vì gốc tài liệu
// chỉ nhận `paperSection`.

const lowlight = createLowlight(common);

export type MathClick = (kind: 'inline' | 'block', node: PmNode, pos: number) => void;

/** Phần dựng cấu trúc tài liệu — dùng chung cho trình soạn và cho việc dựng khung mục ban đầu (cùng schema). */
function contentExtensions(onMathClick?: MathClick): AnyExtension[] {
  return [
    PaperDocument,
    PaperSection,
    StarterKit.configure({
      document: false,
      undoRedo: false,
      trailingNode: false,
      codeBlock: false,
      heading: { levels: [3, 4] },
      link: { openOnClick: false, autolink: true }
    }),
    CodeBlockLowlight.configure({ lowlight, defaultLanguage: 'plaintext' }),
    TableKit.configure({ table: { resizable: false } }),
    Image.configure({ allowBase64: false }),
    Mathematics.configure({
      katexOptions: { throwOnError: false },
      inlineOptions: { onClick: (node, pos) => onMathClick?.('inline', node, pos) },
      blockOptions: { onClick: (node, pos) => onMathClick?.('block', node, pos) }
    }),
    Citation
  ];
}

let schema: ReturnType<typeof getSchema> | null = null;

/** Schema của tài liệu paper (để dựng khung mục ban đầu vào Yjs trước khi mở trình soạn). */
export function paperSchema() {
  schema ??= getSchema(contentExtensions());
  return schema;
}

/** Xem bản nộp (chỉ đọc, không Yjs): cùng cấu trúc tài liệu, không cần chặn sửa mục / gợi ý / cộng tác. */
export function paperViewerExtensions(): AnyExtension[] {
  return contentExtensions();
}

export function paperExtensions(doc: Y.Doc, onMathClick: MathClick): AnyExtension[] {
  return [
    ...contentExtensions(onMathClick),
    SectionGuard,
    Placeholder.configure({ placeholder: 'Viết nội dung mục này…', includeChildren: true }),
    Collaboration.configure({ document: doc })
  ];
}
