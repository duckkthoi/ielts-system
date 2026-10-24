import type { Dayjs } from 'dayjs';

import type { ChecklistKind, CriterionCode, TaskType } from '@/lib/types/assignment';

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
}
