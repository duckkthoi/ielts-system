import { X } from 'lucide-react';

import type { SelectionAction, TextSelection } from './useCommentLayer';

// ─── Thanh công cụ nổi khi bôi đen trên tài liệu Paper (giáo viên nhận xét trực tiếp, H4) ─

export function SelectionToolbar({
  selection,
  actions,
  onAction,
  onCancel
}: {
  selection: TextSelection;
  actions: SelectionAction[];
  onAction: (key: string) => void;
  onCancel: () => void;
}) {
  const button =
    'inline-flex cursor-pointer items-center gap-1 rounded-md border-0 bg-transparent px-2 py-1 text-small text-neutral-700 hover:bg-neutral-50';
  return (
    <div
      role="toolbar"
      aria-label="Thao tác với đoạn đã chọn"
      className="absolute z-20 flex flex-wrap gap-0.5 rounded-lg border border-neutral-100 bg-neutral-0 p-1 shadow-lg"
      style={{ left: selection.left, top: selection.top }}
      onMouseDown={(e) => e.preventDefault()}
    >
      {actions.map((a) => (
        <button key={a.key} type="button" className={button} onClick={() => onAction(a.key)}>
          {a.icon}
          {a.label}
        </button>
      ))}
      <button type="button" className={`${button} text-neutral-500`} onClick={onCancel}>
        <X size={14} />
        Bỏ chọn
      </button>
    </div>
  );
}
