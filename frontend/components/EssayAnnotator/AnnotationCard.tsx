import { Button } from 'antd';
import clsx from 'clsx';
import { ArrowRight, Pencil, Trash2, X } from 'lucide-react';

import { CATEGORY_LABEL, effectiveEdit } from '@/lib/utils/annotations';

import { CATEGORY_DOT, KIND_LABEL } from './const';
import type { AnnotationView } from './type';

// ─── Thẻ chi tiết một đánh dấu ở lề phải: kiểu, loại lỗi, tiêu chí, sửa, nhận xét, Sửa / Xoá ─

export function AnnotationCard({
  ann,
  readOnly,
  onClose,
  onEdit,
  onDelete
}: {
  ann: AnnotationView;
  readOnly: boolean;
  onClose: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
}) {
  const edit = effectiveEdit(ann);
  return (
    <div className="flex flex-col gap-2 rounded-lg border border-brand-200 bg-neutral-0 p-3 text-small shadow-md">
      <div className="flex items-start justify-between gap-2">
        <span className="font-semibold text-neutral-900">{KIND_LABEL[ann.kind]}</span>
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng"
          className="cursor-pointer border-0 bg-transparent p-0 text-neutral-400 hover:text-neutral-700"
        >
          <X size={14} />
        </button>
      </div>
      {ann.errorCode && (
        <span className="inline-flex items-center gap-1.5 text-neutral-700">
          {ann.category && <span className={clsx('size-2 shrink-0 rounded-full', CATEGORY_DOT[ann.category])} />}
          <span className="font-mono font-semibold">{ann.errorCode}</span>
          {ann.errorName ?? (ann.category ? CATEGORY_LABEL[ann.category] : '')}
        </span>
      )}
      {ann.criterionLabel && <span className="text-caption text-neutral-500">Tiêu chí: {ann.criterionLabel}</span>}
      {edit && (
        <span className="flex flex-wrap items-center gap-1 font-serif">
          {edit === 'INSERT' ? (
            <>
              <span className="text-neutral-500">Thêm:</span>
              <span className="text-annot-edit underline">{ann.suggestion}</span>
            </>
          ) : (
            <>
              <span className="text-annot-edit line-through">{ann.quote}</span>
              {edit === 'REPLACE' && (
                <>
                  <ArrowRight size={12} className="text-neutral-400" />
                  <span className="text-annot-edit underline">{ann.suggestion}</span>
                </>
              )}
            </>
          )}
        </span>
      )}
      {ann.comment && <p className="whitespace-pre-wrap text-neutral-700">{ann.comment}</p>}
      {!readOnly && (
        <div className="flex justify-end gap-1">
          <Button size="small" icon={<Pencil size={12} />} onClick={onEdit}>
            Sửa
          </Button>
          <Button size="small" danger icon={<Trash2 size={12} />} onClick={onDelete}>
            Xoá
          </Button>
        </div>
      )}
    </div>
  );
}
