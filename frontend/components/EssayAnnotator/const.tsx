import { CircleSlash, Highlighter, MessageSquarePlus, PenLine, Plus, ThumbsUp, TriangleAlert } from 'lucide-react';

import type { AnnKind, ErrorCategory, Layers } from '@/lib/utils/annotations';

import type { AnnotateAction } from './type';

// Class Tailwind phải viết nguyên văn (không ghép chuỗi) để Tailwind sinh được.

/** Gạch chân gợn sóng theo nhóm lỗi. */
export const WAVY_CLASS: Record<ErrorCategory, string> = {
  grammar: 'underline decoration-wavy decoration-annot-grammar underline-offset-4',
  spelling: 'underline decoration-wavy decoration-annot-spelling underline-offset-4',
  vocabulary: 'underline decoration-wavy decoration-annot-vocabulary underline-offset-4',
  collocation: 'underline decoration-wavy decoration-annot-vocabulary underline-offset-4',
  coherence: 'underline decoration-wavy decoration-annot-coherence underline-offset-4',
  punctuation: 'underline decoration-wavy decoration-annot-punctuation underline-offset-4',
  other: 'underline decoration-wavy decoration-annot-other underline-offset-4'
};

/** Chấm màu nhóm lỗi (danh sách, thẻ chi tiết). */
export const CATEGORY_DOT: Record<ErrorCategory, string> = {
  grammar: 'bg-annot-grammar',
  spelling: 'bg-annot-spelling',
  vocabulary: 'bg-annot-vocabulary',
  collocation: 'bg-annot-vocabulary',
  coherence: 'bg-annot-coherence',
  punctuation: 'bg-annot-punctuation',
  other: 'bg-annot-other'
};

export const KIND_LABEL: Record<AnnKind, string> = {
  ERROR: 'Lỗi',
  HIGHLIGHT: 'Lưu ý',
  GOOD: 'Dùng hay',
  COMMENT: 'Nhận xét'
};

export const LAYER_LABEL: Record<keyof Layers, string> = {
  highlight: 'Tô màu',
  edit: 'Sửa',
  error: 'Lỗi',
  comment: 'Nhận xét'
};

export const TOOLBAR_ACTIONS: { action: AnnotateAction; label: string; icon: React.ReactNode; tone?: string }[] = [
  { action: 'error', label: 'Lỗi', icon: <TriangleAlert size={14} />, tone: 'text-error' },
  { action: 'highlight', label: 'Đánh dấu', icon: <Highlighter size={14} />, tone: 'text-warning' },
  { action: 'good', label: 'Tốt', icon: <ThumbsUp size={14} />, tone: 'text-success' },
  { action: 'comment', label: 'Nhận xét', icon: <MessageSquarePlus size={14} /> },
  { action: 'replace', label: 'Sửa thành…', icon: <PenLine size={14} /> },
  { action: 'delete', label: 'Xoá', icon: <CircleSlash size={14} /> },
  { action: 'insert', label: 'Thêm chữ', icon: <Plus size={14} /> }
];

/** Chiều cao bong bóng ở lề (px) — dùng để xếp không chồng nhau. */
export const BUBBLE_HEIGHT = 28;
/** Chiều cao ước lượng của thẻ chi tiết đang mở (đo lại sau khi vẽ). */
export const CARD_HEIGHT = 170;
