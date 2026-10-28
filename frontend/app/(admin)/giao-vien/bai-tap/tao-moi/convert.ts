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
    kind: d.kind,
    citationStyle: d.citationStyle,
    maxGroupSize: d.maxGroupSize,
    groupLockAt: d.groupLockAt ? parseServerDate(d.groupLockAt) : null,
    parts: d.parts.map((p) => ({
      title: p.title,
      taskType: p.taskType,
      promptText: htmlToText(p.prompt),
      imageUrl: p.imageUrl,
      minWords: p.minWords,
      suggestedMinutes: p.suggestedMinutes,
      partDueAt: p.partDueAt ? parseServerDate(p.partDueAt) : null,
      weight: p.weight,
      criteria: p.criteria.map(criterionToForm),
      examMode: p.examMode,
      examDurationMinutes: p.examDurationMinutes,
      sections: p.sections.map((s) => ({
        sectionKey: s.sectionKey,
        title: s.title,
        required: s.required,
        minWords: s.minWords,
        maxWords: s.maxWords
      }))
    }))
  };
}

const blankToNull = (value: string | null | undefined) => (value?.trim() ? value.trim() : null);

export function formToRequest(f: AssignmentForm): AssignmentRequest {
  const paper = f.kind === 'PAPER';
  return {
    title: f.title.trim(),
    description: blankToNull(f.description),
    openAt: f.openAt?.toISOString() ?? null,
    dueAt: f.dueAt?.toISOString() ?? null,
    allowLate: f.allowLate,
    maxRevisions: f.maxRevisions,
    showSuggestionsOnRevision: f.showSuggestionsOnRevision,
    // Trường của paper chỉ gửi khi là bài PAPER (server báo lỗi nếu bài luận có khung mục).
    kind: f.kind ?? 'ESSAY',
    citationStyle: paper ? (f.citationStyle ?? 'IEEE') : null,
    maxGroupSize: paper ? (f.maxGroupSize ?? null) : null,
    groupLockAt: paper ? (f.groupLockAt?.toISOString() ?? null) : null,
    parts: (f.parts ?? []).map((p) => ({
      title: p.title.trim(),
      taskType: p.taskType,
      prompt: textToHtml(p.promptText ?? ''),
      imageUrl: p.imageUrl || null,
      minWords: p.minWords ?? null,
      suggestedMinutes: p.suggestedMinutes ?? null,
      partDueAt: p.partDueAt?.toISOString() ?? null,
      weight: p.weight ?? null,
      examMode: !paper && !!p.examMode,
      examDurationMinutes: !paper && p.examMode ? (p.examDurationMinutes ?? null) : null,
      sections: paper
        ? (p.sections ?? []).map((s) => ({
            sectionKey: s.sectionKey || null,
            title: s.title.trim(),
            required: s.required,
            minWords: s.minWords ?? null,
            maxWords: s.maxWords ?? null
          }))
        : [],
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
    if (f.kind === 'PAPER' && !p.sections?.length) problems.push(`Giai đoạn ${i + 1} chưa có khung mục`);
    if (f.kind !== 'PAPER' && p.examMode && !p.examDurationMinutes && !p.suggestedMinutes)
      problems.push(`Phần ${i + 1} bật chế độ thi nhưng chưa có thời gian thi`);
  });
  if (!f.dueAt) problems.push('Chưa đặt hạn nộp');
  else if (f.dueAt.valueOf() <= now) problems.push('Hạn nộp đã qua');
  if (f.openAt && f.dueAt && f.openAt.valueOf() >= f.dueAt.valueOf()) problems.push('Thời điểm mở phải trước hạn nộp');
  if (f.kind === 'PAPER' && f.groupLockAt && f.dueAt && f.groupLockAt.valueOf() > f.dueAt.valueOf())
    problems.push('Hạn chốt nhóm phải trước hạn nộp');
  return problems;
}
