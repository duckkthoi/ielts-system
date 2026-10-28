import type { Dayjs } from 'dayjs';

import type { AssignmentKind, ChecklistKind, CitationStyle, CriterionCode, TaskType } from '@/lib/types/assignment';

// ─── Giá trị form của trình soạn (một Form chung cho mọi bước) ────────────────
// Ngày giờ là Dayjs, đề bài là văn bản thường; đổi sang AssignmentRequest khi lưu (convert.ts).

export interface ChecklistItemForm {
  content: string;
  kind: ChecklistKind;
  maxBand?: number | null;
}

export interface CriterionForm {
  code?: CriterionCode | null;
  name: string;
  description?: string | null;
  maxScore: number;
  step: number;
  weight: number;
  checklist: ChecklistItemForm[];
}

/** Một mục trong khung của giai đoạn bài PAPER (BR-29). */
export interface SectionForm {
  sectionKey?: string | null;
  title: string;
  required: boolean;
  minWords?: number | null;
  maxWords?: number | null;
}

export interface PartForm {
  title: string;
  taskType: TaskType;
  /** Văn bản thường; lưu thành HTML. */
  promptText: string;
  imageUrl?: string | null;
  minWords?: number | null;
  suggestedMinutes?: number | null;
  partDueAt?: Dayjs | null;
  weight?: number | null;
  criteria: CriterionForm[];
  examMode?: boolean;
  examDurationMinutes?: number | null;
  sections?: SectionForm[];
}

export interface AssignmentForm {
  title: string;
  description?: string | null;
  openAt?: Dayjs | null;
  dueAt?: Dayjs | null;
  allowLate: boolean;
  maxRevisions: number;
  showSuggestionsOnRevision: boolean;
  parts: PartForm[];
  /** Mức 4: bài luận IELTS hay paper theo nhóm. */
  kind: AssignmentKind;
  citationStyle?: CitationStyle | null;
  maxGroupSize?: number | null;
  groupLockAt?: Dayjs | null;
}
