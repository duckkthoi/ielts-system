import type { AssignmentRequest, CriterionDto, CriterionRequest, TaskType } from '../types/assignment.ts';
import type { QuestionBankDetail, QuestionBankRequest } from '../types/questionBank.ts';
import { htmlToText, textToHtml } from './promptText.ts';

export interface QuestionBankPartInput {
  title: string;
  taskType: TaskType;
  promptText: string;
  imageUrl?: string | null;
  minWords?: number | null;
  suggestedMinutes?: number | null;
  criteria: CriterionRequest[];
}

export interface QuestionBankPartSnapshot extends QuestionBankPartInput {
  imageUrl: string | null;
  minWords: number | null;
  suggestedMinutes: number | null;
}

export interface QuestionBankAssignmentSettings {
  title: string;
  openAt: string | null;
  dueAt: string;
  allowLate: boolean;
  maxRevisions: number;
  showSuggestionsOnRevision: boolean;
}

const blankToNull = (value: string | null | undefined) => (value?.trim() ? value.trim() : null);

/** Bỏ id server khỏi rubric để phần bài nhận một bản sao độc lập. */
export function copyCriteria(criteria: CriterionDto[]): CriterionRequest[] {
  return criteria.map((criterion) => ({
    code: criterion.code,
    name: criterion.name,
    description: criterion.description,
    maxScore: criterion.maxScore,
    step: criterion.step,
    weight: criterion.weight,
    checklist: criterion.checklist.map((item) => ({
      content: item.content,
      kind: item.kind,
      maxBand: item.kind === 'LIMIT' ? item.maxBand : null
    }))
  }));
}

/** Đề ngân hàng → dữ liệu phần trong trình soạn; hạn riêng và trọng số được giữ ở phía gọi. */
export function questionBankToPart(item: QuestionBankDetail): QuestionBankPartSnapshot {
  return {
    title: item.title,
    taskType: item.taskType,
    promptText: htmlToText(item.prompt),
    imageUrl: item.imageUrl,
    minWords: item.minWords,
    suggestedMinutes: item.suggestedMinutes,
    criteria: copyCriteria(item.criteria)
  };
}

/** Tạo nhanh một bài một phần từ đề ngân hàng; đề và rubric được sao chép độc lập. */
export function questionBankToAssignment(
  item: QuestionBankDetail,
  settings: QuestionBankAssignmentSettings
): AssignmentRequest {
  const part = questionBankToPart(item);
  return {
    title: settings.title.trim(),
    description: null,
    openAt: settings.openAt,
    dueAt: settings.dueAt,
    allowLate: settings.allowLate,
    maxRevisions: settings.maxRevisions,
    showSuggestionsOnRevision: settings.showSuggestionsOnRevision,
    parts: [
      {
        title: part.title,
        taskType: part.taskType,
        prompt: textToHtml(part.promptText),
        imageUrl: part.imageUrl,
        minWords: part.minWords,
        suggestedMinutes: part.suggestedMinutes,
        partDueAt: null,
        weight: null,
        criteria: part.criteria
      }
    ]
  };
}

/** Phần đang soạn → request lưu đề; không lưu hạn riêng hay trọng số của bài tập. */
export function partToQuestionBank(part: QuestionBankPartInput, title: string, tags: string[]): QuestionBankRequest {
  return {
    title: title.trim(),
    taskType: part.taskType,
    prompt: textToHtml(part.promptText),
    imageUrl: blankToNull(part.imageUrl),
    minWords: part.minWords ?? null,
    suggestedMinutes: part.suggestedMinutes ?? null,
    tags: tags.map((tag) => tag.trim()).filter(Boolean),
    criteria: (part.criteria ?? []).map((criterion) => ({
      code: criterion.code ?? null,
      name: criterion.name.trim(),
      description: blankToNull(criterion.description),
      maxScore: criterion.maxScore,
      step: criterion.step,
      weight: criterion.weight,
      checklist: (criterion.checklist ?? []).map((item) => ({
        content: item.content.trim(),
        kind: item.kind,
        maxBand: item.kind === 'LIMIT' ? (item.maxBand ?? null) : null
      }))
    }))
  };
}
