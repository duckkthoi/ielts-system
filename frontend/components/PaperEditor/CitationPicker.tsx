'use client';

import { useMemo, useState } from 'react';

import { Button, Checkbox, Empty, Input, Modal } from 'antd';
import { BookMarked } from 'lucide-react';

import type { CitationStyle } from '@/lib/types/assignment';
import type { ReferenceDto } from '@/lib/types/paper';
import { formatReference } from '@/lib/utils/citation';

// ─── Chọn tài liệu để chèn / sửa một trích dẫn ────────────────────────────────
// Chọn một hoặc nhiều tài liệu từ thư viện nhóm (thứ tự trong trích dẫn = thứ tự bấm chọn). Thư viện chưa có tài
// liệu cần → nút mở thư viện để thêm (tra DOI / BibTeX) rồi quay lại.

export interface PickerTarget {
  /** Đang sửa trích dẫn ở vị trí này; không có = chèn mới ở con trỏ. */
  pos?: number;
  ids: number[];
}

function PickerBody({
  target,
  refs,
  style,
  onSave,
  onRemove,
  onOpenLibrary,
  onClose
}: {
  target: PickerTarget;
  refs: ReferenceDto[];
  style: CitationStyle;
  onSave: (ids: number[]) => void;
  onRemove: () => void;
  onOpenLibrary: () => void;
  onClose: () => void;
}) {
  const [selected, setSelected] = useState<number[]>(target.ids);
  const [query, setQuery] = useState('');
  const editing = target.pos !== undefined;

  const rows = useMemo(() => refs.map((r) => ({ id: r.id, text: formatReference(r.csl, style) })), [refs, style]);
  const shown = useMemo(() => {
    const q = query.trim().toLowerCase();
    return q ? rows.filter((r) => r.text.toLowerCase().includes(q)) : rows;
  }, [rows, query]);

  const toggle = (id: number) => setSelected((cur) => (cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id]));

  return (
    <div className="flex flex-col gap-3">
      <Input.Search
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Tìm theo tác giả, tiêu đề, năm…"
        allowClear
        aria-label="Tìm tài liệu"
      />
      {refs.length === 0 ? (
        <Empty description="Thư viện của nhóm chưa có tài liệu nào.">
          <Button icon={<BookMarked size={16} />} onClick={onOpenLibrary}>
            Mở thư viện để thêm
          </Button>
        </Empty>
      ) : (
        <ul className="m-0 flex max-h-96 list-none flex-col gap-1 overflow-y-auto p-0">
          {shown.map((r) => (
            <li key={r.id}>
              <label className="flex cursor-pointer items-start gap-2 rounded-md px-2 py-1.5 hover:bg-neutral-50">
                <Checkbox checked={selected.includes(r.id)} onChange={() => toggle(r.id)} className="mt-0.5" />
                <span className="font-serif text-body text-neutral-900">{r.text}</span>
              </label>
            </li>
          ))}
          {shown.length === 0 && <li className="px-2 py-3 text-small text-neutral-500">Không có tài liệu khớp.</li>}
        </ul>
      )}
      <div className="flex flex-wrap items-center justify-end gap-2">
        {refs.length > 0 && (
          <Button type="link" className="mr-auto px-0" icon={<BookMarked size={16} />} onClick={onOpenLibrary}>
            Thêm tài liệu vào thư viện
          </Button>
        )}
        {editing && (
          <Button danger onClick={onRemove}>
            Bỏ trích dẫn
          </Button>
        )}
        <Button onClick={onClose}>Huỷ</Button>
        <Button type="primary" disabled={selected.length === 0} onClick={() => onSave(selected)}>
          {editing ? 'Lưu' : `Chèn${selected.length > 1 ? ` ${selected.length} tài liệu` : ''}`}
        </Button>
      </div>
    </div>
  );
}

export function CitationPicker({
  target,
  ...rest
}: {
  target: PickerTarget | null;
  refs: ReferenceDto[];
  style: CitationStyle;
  onSave: (ids: number[]) => void;
  onRemove: () => void;
  onOpenLibrary: () => void;
  onClose: () => void;
}) {
  return (
    <Modal
      open={target !== null}
      title={target?.pos !== undefined ? 'Sửa trích dẫn' : 'Chèn trích dẫn'}
      onCancel={rest.onClose}
      footer={null}
      destroyOnHidden
      width={680}
    >
      {target && <PickerBody key={`${target.pos ?? 'new'}-${target.ids.join(',')}`} target={target} {...rest} />}
    </Modal>
  );
}
