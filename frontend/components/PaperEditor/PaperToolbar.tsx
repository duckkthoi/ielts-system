'use client';

import { useRef, useState } from 'react';

import { App, Divider, Dropdown } from 'antd';
import type { Editor } from '@tiptap/react';
import { useEditorState } from '@tiptap/react';
import {
  Bold,
  BookMarked,
  Code2,
  Heading3,
  Heading4,
  ImagePlus,
  Italic,
  List,
  ListOrdered,
  Pilcrow,
  Quote,
  Redo2,
  Sigma,
  Table,
  Underline,
  Undo2
} from 'lucide-react';

import { IconButton } from '@/components/IconButton';
import { fileApi, IMAGE_ACCEPT, IMAGE_MAX_BYTES } from '@/lib/api/file';

import type { MathTarget } from './MathModal';

// ─── Thanh công cụ của trình soạn paper ───────────────────────────────────────
// Nút sáng lên theo vị trí con trỏ (useEditorState). Đang ở trong bảng thì có thêm menu sửa bảng.

export function PaperToolbar({
  editor,
  onMath,
  onCite
}: {
  editor: Editor;
  onMath: (target: MathTarget) => void;
  /** Có thư viện trích dẫn → nút chèn trích dẫn. */
  onCite?: () => void;
}) {
  const { message } = App.useApp();
  const fileRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const s = useEditorState({
    editor,
    selector: ({ editor: e }) => ({
      paragraph: e.isActive('paragraph'),
      h3: e.isActive('heading', { level: 3 }),
      h4: e.isActive('heading', { level: 4 }),
      bold: e.isActive('bold'),
      italic: e.isActive('italic'),
      underline: e.isActive('underline'),
      bullet: e.isActive('bulletList'),
      ordered: e.isActive('orderedList'),
      quote: e.isActive('blockquote'),
      code: e.isActive('codeBlock'),
      table: e.isActive('table'),
      canUndo: e.can().undo(),
      canRedo: e.can().redo()
    })
  });

  const chain = () => editor.chain().focus();
  const btn = (title: string, icon: React.ReactNode, active: boolean, run: () => void, disabled = false) => (
    <IconButton
      title={title}
      type={active ? 'primary' : 'text'}
      ghost={active}
      size="small"
      icon={icon}
      disabled={disabled}
      onClick={run}
    />
  );

  const onPickImage = async (file: File | undefined) => {
    if (!file) return;
    if (file.size > IMAGE_MAX_BYTES) {
      message.error('Ảnh quá lớn, tối đa 5 MB.');
      return;
    }
    setUploading(true);
    try {
      const url = await fileApi.uploadImage(file);
      chain().setImage({ src: url, alt: file.name }).run();
    } catch {
      // axiosClient đã toast lý do
    } finally {
      setUploading(false);
    }
  };

  const tableItems = [
    { key: 'rowAfter', label: 'Thêm hàng bên dưới', run: () => chain().addRowAfter().run() },
    { key: 'colAfter', label: 'Thêm cột bên phải', run: () => chain().addColumnAfter().run() },
    { key: 'header', label: 'Bật / tắt hàng tiêu đề', run: () => chain().toggleHeaderRow().run() },
    { key: 'delRow', label: 'Xoá hàng', run: () => chain().deleteRow().run() },
    { key: 'delCol', label: 'Xoá cột', run: () => chain().deleteColumn().run() },
    { key: 'delTable', label: 'Xoá bảng', danger: true, run: () => chain().deleteTable().run() }
  ];

  return (
    <div
      role="toolbar"
      aria-label="Định dạng"
      className="flex flex-wrap items-center gap-0.5 border-b border-neutral-100 px-2 py-1.5 sm:px-4"
    >
      {btn('Hoàn tác (Ctrl+Z)', <Undo2 size={16} />, false, () => chain().undo().run(), !s.canUndo)}
      {btn('Làm lại (Ctrl+Y)', <Redo2 size={16} />, false, () => chain().redo().run(), !s.canRedo)}
      <Divider orientation="vertical" />
      {btn('Đoạn văn', <Pilcrow size={16} />, s.paragraph, () => chain().setParagraph().run())}
      {btn('Tiêu đề nhỏ', <Heading3 size={16} />, s.h3, () => chain().toggleHeading({ level: 3 }).run())}
      {btn('Tiêu đề con', <Heading4 size={16} />, s.h4, () => chain().toggleHeading({ level: 4 }).run())}
      <Divider orientation="vertical" />
      {btn('Đậm (Ctrl+B)', <Bold size={16} />, s.bold, () => chain().toggleBold().run())}
      {btn('Nghiêng (Ctrl+I)', <Italic size={16} />, s.italic, () => chain().toggleItalic().run())}
      {btn('Gạch chân (Ctrl+U)', <Underline size={16} />, s.underline, () => chain().toggleUnderline().run())}
      <Divider orientation="vertical" />
      {btn('Danh sách', <List size={16} />, s.bullet, () => chain().toggleBulletList().run())}
      {btn('Danh sách đánh số', <ListOrdered size={16} />, s.ordered, () => chain().toggleOrderedList().run())}
      {btn('Trích khối', <Quote size={16} />, s.quote, () => chain().toggleBlockquote().run())}
      {btn('Khối code', <Code2 size={16} />, s.code, () => chain().toggleCodeBlock().run())}
      <Divider orientation="vertical" />
      {s.table ? (
        <Dropdown
          trigger={['click']}
          menu={{
            items: tableItems.map(({ key, label, danger }) => ({ key, label, danger })),
            onClick: ({ key }) => tableItems.find((i) => i.key === key)?.run()
          }}
        >
          <IconButton title="Sửa bảng" type="primary" ghost size="small" icon={<Table size={16} />} />
        </Dropdown>
      ) : (
        btn('Chèn bảng 3 × 3', <Table size={16} />, false, () =>
          chain().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
        )
      )}
      <IconButton
        title="Chèn hình (PNG, JPG, WEBP ≤ 5 MB)"
        type="text"
        size="small"
        loading={uploading}
        icon={<ImagePlus size={16} />}
        onClick={() => fileRef.current?.click()}
      />
      <input
        ref={fileRef}
        type="file"
        accept={IMAGE_ACCEPT}
        className="hidden"
        onChange={(e) => {
          void onPickImage(e.target.files?.[0]);
          e.target.value = '';
        }}
      />
      <Dropdown
        trigger={['click']}
        menu={{
          items: [
            { key: 'inline', label: 'Công thức trong dòng' },
            { key: 'block', label: 'Công thức riêng dòng' }
          ],
          onClick: ({ key }) => onMath({ kind: key === 'block' ? 'block' : 'inline', latex: '' })
        }}
      >
        <IconButton title="Chèn công thức (LaTeX)" type="text" size="small" icon={<Sigma size={16} />} />
      </Dropdown>
      {onCite && (
        <>
          <Divider orientation="vertical" />
          <IconButton
            title="Chèn trích dẫn"
            type="text"
            size="small"
            icon={<BookMarked size={16} />}
            onClick={onCite}
          />
        </>
      )}
    </div>
  );
}
