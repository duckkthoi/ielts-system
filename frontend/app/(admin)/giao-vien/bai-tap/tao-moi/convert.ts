import type { AssignmentDetail, AssignmentRequest, CriterionDto } from '@/lib/types/assignment';
import { parseServerDate } from '@/lib/utils/date';
import { htmlToText, textToHtml } from '@/lib/utils/promptText';

import type { AssignmentForm, CriterionForm } from './type';

// ─── Đổi qua lại giữa dữ liệu server và giá trị form của trình soạn ───────────

/** Tiêu chí của server / rubric mẫu → tiêu chí trong form (bỏ id, giữ thứ tự). */
export function criterionToForm(c: CriterionDto): CriterionForm {
  return {
    code: c.code,
    name: c.name,
    description: c.description ?? '',
    maxScore: c.maxScore,
    step: c.step,
    weight: c.weight,
    checklist: c.checklist.map((i) => ({ content: i.content, kind: i.kind, maxBand: i.maxBand }))
  };
}

export function detailToForm(d: AssignmentDetail): AssignmentForm {
  return {
    title: d.title,
    description: d.description ?? '',
    openAt: d.openAt ? parseServerDate(d.openAt) : null,
    dueAt: d.dueAt ? parseServerDate(d.dueAt) : null,
    allowLate: d.allowLate,
    maxRevisions: d.maxRevisions,
    showSuggestionsOnRevision: d.showSuggestionsOnRevision,
    parts: d.parts.map((p) => ({
      title: p.title,
      taskType: p.taskType,
      promptText: htmlToText(p.prompt),
      imageUrl: p.imageUrl,
      minWords: p.minWords,
      suggestedMinutes: p.suggestedMinutes,
      partDueAt: p.partDueAt ? parseServerDate(p.partDueAt) : null,
      weight: p.weight,
      criteria: p.criteria.map(criterionToForm)
    }))
  };
}

const blankToNull = (value: string | null | undefined) => (value?.trim() ? value.trim() : null);

export function formToRequest(f: AssignmentForm): AssignmentRequest {
  return {
    title: f.title.trim(),
    description: blankToNull(f.description),
    openAt: f.openAt?.toISOString() ?? null,
    dueAt: f.dueAt?.toISOString() ?? null,
    allowLate: f.allowLate,
    maxRevisions: f.maxRevisions,
    showSuggestionsOnRevision: f.showSuggestionsOnRevision,
    parts: (f.parts ?? []).map((p) => ({
      title: p.title.trim(),
      taskType: p.taskType,
      prompt: textToHtml(p.promptText ?? ''),
      imageUrl: p.imageUrl || null,
      minWords: p.minWords ?? null,
      suggestedMinutes: p.suggestedMinutes ?? null,
      partDueAt: p.partDueAt?.toISOString() ?? null,
      weight: p.weight ?? null,
      criteria: (p.criteria ?? []).map((c) => ({
        code: c.code || null,
        name: c.name.trim(),
        description: blankToNull(c.description),
        maxScore: c.maxScore,
        step: c.step,
        weight: c.weight,
        checklist: (c.checklist ?? []).map((i) => ({
          content: i.content.trim(),
          kind: i.kind,
          maxBand: i.kind === 'LIMIT' ? (i.maxBand ?? null) : null
        }))
      }))
    }))
  };
}

/**
 * Điều kiện phát hành kiểm trước ở client để hiện danh sách việc còn thiếu (server kiểm lại khi phát hành, UC-14).
 * Không chặn lưu nháp.
 */
export function readinessProblems(f: AssignmentForm, now: number = Date.now()): string[] {
  const problems: string[] = [];
  const parts = f.parts ?? [];
  if (parts.length === 0) problems.push('Bài chưa có phần nào');
  parts.forEach((p, i) => {
    if (!p.promptText?.trim()) problems.push(`Phần ${i + 1} chưa có đề bài`);
    if (!p.criteria?.length) problems.push(`Phần ${i + 1} chưa có tiêu chí chấm`);
    if (p.partDueAt && p.partDueAt.valueOf() <= now) problems.push(`Hạn riêng của phần ${i + 1} đã qua`);
  });
  if (!f.dueAt) problems.push('Chưa đặt hạn nộp');
  else if (f.dueAt.valueOf() <= now) problems.push('Hạn nộp đã qua');
  if (f.openAt && f.dueAt && f.openAt.valueOf() >= f.dueAt.valueOf()) problems.push('Thời điểm mở phải trước hạn nộp');
  return problems;
}
