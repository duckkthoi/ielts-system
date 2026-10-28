'use client';

import { useState } from 'react';

import { Alert, Button, Input, Modal, Segmented, Select } from 'antd';

import type { AnnotateAction } from '@/components/EssayAnnotator';
import type { CriterionGroup, EditOp, ErrorCodeDto, GradingCriterion, InlineKind } from '@/lib/types/grading';
import { QUICK_CATEGORIES } from '@/lib/utils/annotations';

import { CommentBankPicker } from './CommentBankPicker';
import type { InlineDraft } from './useGradeForm';

// ─── Tạo / sửa một đánh dấu: kiểu, loại lỗi (7 nút nhanh + mã IELTS), sửa, tiêu chí, nhận xét ─
// Kiểm tra giống server (lỗi cần mã / nhận xét / sửa; sửa thành / thêm chữ cần chữ mới; nhận xét không rỗng).

const KIND_OPTIONS: { value: InlineKind; label: string }[] = [
  { value: 'ERROR', label: 'Lỗi' },
  { value: 'HIGHLIGHT', label: 'Lưu ý (vàng)' },
  { value: 'GOOD', label: 'Dùng hay (xanh)' },
  { value: 'COMMENT', label: 'Nhận xét' }
];

const EDIT_OPTIONS: { value: EditOp | 'NONE'; label: string }[] = [
  { value: 'NONE', label: 'Không sửa' },
  { value: 'REPLACE', label: 'Sửa thành…' },
  { value: 'DELETE', label: 'Xoá' },
  { value: 'INSERT', label: 'Thêm chữ' }
];

const GROUP_LABEL: Record<CriterionGroup, string> = {
  TASK: 'Nội dung (TA / TR)',
  CC: 'Liên kết (CC)',
  LR: 'Từ vựng (LR)',
  GRA: 'Ngữ pháp (GRA)'
};

export interface AnnotationFormTarget {
  range: { start: number; end: number };
  quote: string;
  /** Có khi đang sửa đánh dấu cũ. */
  key?: string;
  draft?: InlineDraft;
  action?: AnnotateAction;
}

function initial(t: AnnotationFormTarget) {
  if (t.draft) {
    return {
      kind: t.draft.kind,
      errorCode: t.draft.errorCode,
      criterionId: t.draft.criterionId,
      comment: t.draft.comment ?? '',
      edit: (t.draft.edit ?? (t.draft.kind === 'ERROR' && t.draft.suggestion ? 'REPLACE' : 'NONE')) as EditOp | 'NONE',
      text: t.draft.suggestion ?? ''
    };
  }
  const a = t.action;
  return {
    kind: (a === 'comment' ? 'COMMENT' : 'ERROR') as InlineKind,
    errorCode: null as string | null,
    criterionId: null as number | null,
    comment: '',
    edit: (a === 'replace' ? 'REPLACE' : a === 'insert' ? 'INSERT' : 'NONE') as EditOp | 'NONE',
    text: a === 'replace' ? t.quote : ''
  };
}

export function AnnotationFormModal({
  target,
  errorCodes,
  criteria,
  onCancel,
  onSave
}: {
  target: AnnotationFormTarget;
  errorCodes: ErrorCodeDto[];
  criteria: GradingCriterion[];
  onCancel: () => void;
  onSave: (draft: InlineDraft) => void;
}) {
  const [v, setV] = useState(() => initial(target));
  const [error, setError] = useState<string | null>(null);
  const set = (patch: Partial<typeof v>) => {
    setV((x) => ({ ...x, ...patch }));
    setError(null);
  };

  const isError = v.kind === 'ERROR';
  const needsText = isError && (v.edit === 'REPLACE' || v.edit === 'INSERT');
  const codeGroup = errorCodes.find((c) => c.code === v.errorCode)?.criterion ?? null;
  const group = criteria.find((c) => c.id === v.criterionId)?.group ?? codeGroup;
  const detailOptions = (['GRA', 'LR', 'CC', 'TASK'] as CriterionGroup[]).map((g) => ({
    label: GROUP_LABEL[g],
    options: errorCodes
      .filter((c) => !c.positive && c.criterion === g)
      .map((c) => ({ value: c.code, label: `${c.code} — ${c.name}` }))
  }));
  const otherCodes = errorCodes.filter((c) => !c.positive && !c.criterion);
  if (otherCodes.length) {
    detailOptions.push({
      label: 'Khác',
      options: otherCodes.map((c) => ({ value: c.code, label: `${c.code} — ${c.name}` }))
    });
  }

  const submit = () => {
    const comment = v.comment.trim() || null;
    const text = v.text.trim();
    if (v.kind === 'COMMENT' && !comment) return setError('Nhập nội dung nhận xét');
    if (needsText && !text) return setError(v.edit === 'INSERT' ? 'Nhập chữ cần thêm' : 'Nhập chữ sửa thành');
    if (isError && !v.errorCode && !comment && v.edit === 'NONE') {
      return setError('Chọn loại lỗi, ghi nhận xét hoặc chọn cách sửa');
    }
    onSave({
      startOffset: target.range.start,
      endOffset: target.range.end,
      quote: target.quote,
      kind: v.kind,
      errorCode: isError ? v.errorCode : null,
      criterionId: isError ? v.criterionId : null,
      comment,
      suggestion: needsText ? text : null,
      edit: isError && v.edit !== 'NONE' ? v.edit : null
    });
  };

  return (
    <Modal
      open
      title={target.key ? 'Sửa đánh dấu' : 'Thêm đánh dấu'}
      okText="Lưu"
      cancelText="Huỷ"
      onOk={submit}
      onCancel={onCancel}
      width={560}
      destroyOnHidden
    >
      <div className="flex flex-col gap-4">
        <blockquote className="m-0 max-h-24 overflow-y-auto rounded-md border-l-4 border-brand-500 bg-neutral-25 px-3 py-2 font-serif text-neutral-900">
          {target.quote}
        </blockquote>

        <Segmented block value={v.kind} options={KIND_OPTIONS} onChange={(k) => set({ kind: k as InlineKind })} />

        {isError && (
          <>
            <div className="flex flex-col gap-2">
              <span className="font-semibold text-neutral-900">Loại lỗi</span>
              <div className="flex flex-wrap gap-1.5">
                {QUICK_CATEGORIES.map((c) => (
                  <Button
                    key={c.code}
                    size="small"
                    type={v.errorCode === c.code ? 'primary' : 'default'}
                    onClick={() => set({ errorCode: v.errorCode === c.code ? null : c.code })}
                  >
                    {c.label}
                  </Button>
                ))}
              </div>
              <Select
                allowClear
                showSearch={{ optionFilterProp: 'label' }}
                placeholder="Hoặc chọn mã IELTS chi tiết (VT, SV, ART…)"
                value={v.errorCode ?? undefined}
                options={detailOptions}
                onChange={(code?: string) => set({ errorCode: code ?? null })}
              />
            </div>

            <div className="flex flex-col gap-2">
              <span className="font-semibold text-neutral-900">Sửa (chữ gốc của sinh viên vẫn giữ nguyên)</span>
              <Segmented value={v.edit} options={EDIT_OPTIONS} onChange={(e) => set({ edit: e as EditOp | 'NONE' })} />
              {needsText && (
                <Input
                  autoFocus
                  value={v.text}
                  maxLength={1000}
                  onChange={(e) => set({ text: e.target.value })}
                  placeholder={v.edit === 'INSERT' ? 'Chữ thêm vào ngay sau đoạn đã chọn' : 'Sửa thành…'}
                />
              )}
            </div>

            <Select
              allowClear
              placeholder="Tiêu chí: tự động theo mã lỗi"
              value={v.criterionId ?? undefined}
              options={criteria.map((c) => ({ value: c.id, label: c.code ? `${c.code} — ${c.name}` : c.name }))}
              onChange={(id?: number) => set({ criterionId: id ?? null })}
            />
          </>
        )}

        <div className="flex flex-col gap-1">
          <div className="flex items-center justify-between">
            <span className="font-semibold text-neutral-900">Nhận xét{v.kind === 'COMMENT' ? '' : ' (tuỳ chọn)'}</span>
            <CommentBankPicker
              group={group}
              current={v.comment}
              onInsert={(t) => set({ comment: v.comment ? `${v.comment.trimEnd()} ${t}` : t })}
            />
          </div>
          <Input.TextArea
            value={v.comment}
            maxLength={2000}
            autoSize={{ minRows: 2, maxRows: 6 }}
            onChange={(e) => set({ comment: e.target.value })}
            placeholder="Giải thích ngắn cho sinh viên…"
          />
        </div>

        {error && <Alert type="error" showIcon title={error} />}
      </div>
    </Modal>
  );
}
