import type { CriterionCode, TaskType } from '@/lib/types/assignment';

import type { AssignmentForm, CriterionForm, PartForm } from './type';

export const STEPS = [
  { key: 'general', title: 'Thông tin chung' },
  { key: 'parts', title: 'Các phần' },
  { key: 'criteria', title: 'Tiêu chí & checklist' },
  { key: 'publish', title: 'Giao bài' }
] as const;

/** Giới hạn của server (docs/05 A.5). */
export const MAX_PARTS = 10;
export const MAX_CRITERIA = 10;
export const MAX_CHECKLIST = 30;

/** Số từ tối thiểu + thời gian gợi ý mặc định theo loại phần (đề IELTS chuẩn). */
const PART_DEFAULTS: Record<TaskType, { title: string; minWords: number | null; suggestedMinutes: number | null }> = {
  WRITING_TASK1: { title: 'Task 1', minWords: 150, suggestedMinutes: 20 },
  WRITING_TASK2: { title: 'Task 2', minWords: 250, suggestedMinutes: 40 },
  CUSTOM: { title: 'Phần tự do', minWords: null, suggestedMinutes: null }
};

export function newPart(taskType: TaskType): PartForm {
  const d = PART_DEFAULTS[taskType];
  return {
    title: d.title,
    taskType,
    promptText: '',
    imageUrl: null,
    minWords: d.minWords,
    suggestedMinutes: d.suggestedMinutes,
    partDueAt: null,
    weight: null,
    criteria: []
  };
}

/** Đổi loại phần → đổi số từ / thời gian mặc định (chỉ khi giáo viên chưa sửa khác mặc định cũ). */
export function partDefaults(taskType: TaskType) {
  return PART_DEFAULTS[taskType];
}

export function newCriterion(): CriterionForm {
  return { code: null, name: '', description: '', maxScore: 9, step: 1, weight: 1, checklist: [] };
}

export const EMPTY_FORM: AssignmentForm = {
  title: '',
  description: '',
  openAt: null,
  dueAt: null,
  allowLate: true,
  maxRevisions: 2,
  showSuggestionsOnRevision: false,
  parts: [newPart('WRITING_TASK1'), newPart('WRITING_TASK2')]
};

export const TASK_TYPE_OPTIONS: { value: TaskType; label: string }[] = [
  { value: 'WRITING_TASK1', label: 'IELTS Writing Task 1' },
  { value: 'WRITING_TASK2', label: 'IELTS Writing Task 2' },
  { value: 'CUSTOM', label: 'Tự do' }
];

export const CRITERION_CODE_OPTIONS: { value: CriterionCode | ''; label: string }[] = [
  { value: '', label: 'Tự định nghĩa' },
  { value: 'TA', label: 'TA · Task Achievement' },
  { value: 'TR', label: 'TR · Task Response' },
  { value: 'CC', label: 'CC · Coherence and Cohesion' },
  { value: 'LR', label: 'LR · Lexical Resource' },
  { value: 'GRA', label: 'GRA · Grammatical Range and Accuracy' }
];
