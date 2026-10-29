'use client';

import { useEffect, useRef, useState } from 'react';

import { Button, Input, Modal } from 'antd';
import katex from 'katex';

// ─── Nhập / sửa công thức LaTeX, xem trước bằng KaTeX ─────────────────────────
// Xem trước vẽ thẳng vào phần tử (katex.render), không qua dangerouslySetInnerHTML.

export interface MathTarget {
  kind: 'inline' | 'block';
  latex: string;
  /** Có `pos` = đang sửa công thức đã có; không có = chèn mới ở con trỏ. */
  pos?: number;
}

type Actions = { onSave: (latex: string) => void; onDelete: () => void; onClose: () => void };

/** Thân hộp thoại — gắn lại mỗi lần mở nên ô nhập lấy đúng công thức đang sửa. */
function MathForm({ target, onSave, onDelete, onClose }: { target: MathTarget } & Actions) {
  const [latex, setLatex] = useState(target.latex);
  const previewRef = useRef<HTMLDivElement>(null);
  const editing = target.pos !== undefined;

  useEffect(() => {
    if (!previewRef.current) return;
    katex.render(latex || '\\;', previewRef.current, { throwOnError: false, displayMode: target.kind === 'block' });
  }, [latex, target.kind]);

  return (
    <div className="flex flex-col gap-3">
      <Input.TextArea
        value={latex}
        onChange={(e) => setLatex(e.target.value)}
        autoSize={{ minRows: 2, maxRows: 6 }}
        placeholder="Mã LaTeX, vd \frac{a}{b} hoặc E = mc^2"
        aria-label="Mã LaTeX"
        className="font-mono"
        autoFocus
      />
      <div className="min-h-12 overflow-x-auto rounded-md border border-neutral-100 bg-neutral-25 px-3 py-2">
        <div ref={previewRef} aria-label="Xem trước công thức" />
      </div>
      <div className="flex justify-end gap-2">
        {editing && (
          <Button danger className="mr-auto" onClick={onDelete}>
            Xoá công thức
          </Button>
        )}
        <Button onClick={onClose}>Huỷ</Button>
        <Button type="primary" disabled={!latex.trim()} onClick={() => onSave(latex.trim())}>
          {editing ? 'Lưu' : 'Chèn'}
        </Button>
      </div>
    </div>
  );
}

export function MathModal({ target, ...actions }: { target: MathTarget | null } & Actions) {
  return (
    <Modal
      open={target !== null}
      title={`${target?.pos !== undefined ? 'Sửa' : 'Chèn'} công thức ${target?.kind === 'block' ? 'riêng dòng' : 'trong dòng'}`}
      onCancel={actions.onClose}
      destroyOnHidden
      footer={null}
    >
      {target && <MathForm key={`${target.kind}-${target.pos ?? 'new'}`} target={target} {...actions} />}
    </Modal>
  );
}
