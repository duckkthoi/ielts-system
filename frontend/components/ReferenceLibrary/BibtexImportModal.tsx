'use client';

import { useMemo, useState } from 'react';

import { Alert, Button, Input, Modal, Upload } from 'antd';
import { FileUp } from 'lucide-react';

import type { CitationStyle } from '@/lib/types/assignment';
import type { CslItem } from '@/lib/types/paper';
import { formatReference, parseBibtex } from '@/lib/utils/citation';

// ─── Nhập BibTeX: dán chữ hoặc chọn file .bib → xem trước → nhập cả lô ────────
// Đọc file ngay trên trình duyệt (không upload). Mục có DOI đã có trong thư viện do server bỏ qua.

const MAX_ITEMS = 100;
const MAX_FILE_BYTES = 1024 * 1024;

export function BibtexImportModal({
  open,
  style,
  saving,
  onImport,
  onClose
}: {
  open: boolean;
  style: CitationStyle;
  saving: boolean;
  onImport: (items: CslItem[]) => void;
  onClose: () => void;
}) {
  const [text, setText] = useState('');
  const [fileError, setFileError] = useState<string | null>(null);

  const parsed = useMemo((): { items: CslItem[]; error: string | null } => {
    if (!text.trim()) return { items: [], error: null };
    try {
      return { items: parseBibtex(text), error: null };
    } catch (e) {
      return { items: [], error: (e as Error).message };
    }
  }, [text]);
  const tooMany = parsed.items.length > MAX_ITEMS;

  const readFile = (file: File) => {
    setFileError(null);
    if (file.size > MAX_FILE_BYTES) {
      setFileError('File quá lớn, tối đa 1 MB.');
      return false;
    }
    void file.text().then(setText);
    return false; // không upload — chỉ đọc trên máy
  };

  const close = () => {
    setText('');
    setFileError(null);
    onClose();
  };

  return (
    <Modal
      open={open}
      title="Nhập từ BibTeX"
      onCancel={close}
      destroyOnHidden
      width={720}
      footer={[
        <Button key="cancel" onClick={close}>
          Huỷ
        </Button>,
        <Button
          key="ok"
          type="primary"
          loading={saving}
          disabled={parsed.items.length === 0 || tooMany}
          onClick={() => onImport(parsed.items)}
        >
          {parsed.items.length ? `Nhập ${parsed.items.length} tài liệu` : 'Nhập'}
        </Button>
      ]}
    >
      <div className="flex flex-col gap-3">
        <p className="m-0 text-small text-neutral-500">
          Dán BibTeX xuất từ Google Scholar, Zotero, Mendeley… hoặc chọn file <code>.bib</code>.
        </p>
        <Upload accept=".bib,.txt,text/plain" showUploadList={false} beforeUpload={readFile} maxCount={1}>
          <Button icon={<FileUp size={16} />}>Chọn file .bib</Button>
        </Upload>
        {fileError && <Alert type="error" showIcon title={fileError} />}
        <Input.TextArea
          value={text}
          onChange={(e) => setText(e.target.value)}
          autoSize={{ minRows: 6, maxRows: 14 }}
          placeholder={
            '@article{lecun2015,\n  title = {Deep learning},\n  author = {LeCun, Yann and Bengio, Yoshua},\n  ...\n}'
          }
          className="font-mono text-small"
          aria-label="Nội dung BibTeX"
        />
        {parsed.error && <Alert type="error" showIcon title={parsed.error} />}
        {tooMany && <Alert type="warning" showIcon title={`Mỗi lần nhập tối đa ${MAX_ITEMS} tài liệu.`} />}
        {parsed.items.length > 0 && (
          <div className="max-h-64 overflow-y-auto rounded-md border border-neutral-100 bg-neutral-25 p-3">
            <p className="m-0 mb-2 text-caption text-neutral-500">
              Xem trước {parsed.items.length} tài liệu theo kiểu {style}:
            </p>
            <ol className="m-0 flex list-decimal flex-col gap-1.5 pl-5 text-small text-neutral-900">
              {parsed.items.map((it, i) => (
                <li key={i}>{formatReference(it, style)}</li>
              ))}
            </ol>
          </div>
        )}
      </div>
    </Modal>
  );
}
