'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { gradingApi } from '@/lib/api/grading';
import { getErrorMessage } from '@/lib/apiError';
import type { LevelDto } from '@/lib/types/assignment';
import type {
  EditOp,
  GradeDto,
  GradingCriterion,
  GradingScreen,
  InlineKind,
  SaveGradeRequest,
  ZeroReason
} from '@/lib/types/grading';
import {
  clampScore,
  levelOfScore,
  paperScore,
  partBandRaw,
  roundBand,
  scoreCap,
  scoreForLevel
} from '@/lib/utils/grading';

// ─── Form chấm một version (SD-08) ────────────────────────────────────────────
// Giữ điểm / checklist / nhận xét trên client, tự lưu nháp sau 2 giây ngừng sửa (PUT …/grade thay toàn bộ).
// Giới hạn điểm và band chỉ tính để XEM TRƯỚC (BR-12, BR-17, BR-18); server tính lại khi lưu.
// Đánh dấu trên bài (F5b) cũng nằm trong form: thêm / sửa / xoá rồi tự lưu như điểm.
// Bài PAPER (B25c-2, BR-31): thêm mức đạt từng tiêu chí và cộng / trừ riêng từng thành viên; điểm thang 10.

export type GradeSaveState = 'SAVED' | 'DIRTY' | 'SAVING' | 'ERROR';

const DEBOUNCE_MS = 2_000;

export interface GradeForm {
  scores: Record<number, number | null>;
  comments: Record<number, string>;
  checked: Record<number, boolean>;
  generalComment: string;
  zeroReason: ZeroReason | null;
  /** Đánh dấu trên bài; `key` ổn định phía client (id server đổi sau mỗi lần lưu vì PUT thay toàn bộ). */
  inline: LocalInline[];
  /** Bài PAPER: mức đạt đã chọn theo tiêu chí. */
  levels: Record<number, number | null>;
  /** Bài PAPER: cộng / trừ theo thành viên; dòng chưa có lý do chưa được gửi (server bắt buộc lý do). */
  adjustments: Record<number, { delta: number; reason: string }>;
}

export interface LocalInline {
  key: string;
  startOffset: number;
  endOffset: number;
  quote: string;
  kind: InlineKind;
  errorCode: string | null;
  criterionId: number | null;
  comment: string | null;
  suggestion: string | null;
  edit: EditOp | null;
}

export type InlineDraft = Omit<LocalInline, 'key'>;

const isPaper = (screen: GradingScreen) => screen.assignment.kind === 'PAPER';

/** Dòng cộng / trừ đủ điều kiện gửi: có delta và có lý do. */
export const readyAdjustment = (a: { delta: number; reason: string } | undefined) =>
  a !== undefined && a.delta !== 0 && a.reason.trim() !== '';

function initForm(screen: GradingScreen): GradeForm {
  const g = screen.grade;
  const form: GradeForm = {
    scores: {},
    comments: {},
    checked: {},
    generalComment: g?.generalComment ?? '',
    zeroReason: g?.zeroReason ?? null,
    inline: (g?.inlineComments ?? []).map((ic) => ({
      key: `s${ic.id}`,
      startOffset: ic.startOffset,
      endOffset: ic.endOffset,
      quote: ic.quote,
      kind: ic.kind,
      errorCode: ic.errorCode,
      criterionId: ic.criterionId,
      comment: ic.comment,
      suggestion: ic.suggestion,
      edit: ic.edit
    })),
    levels: {},
    adjustments: Object.fromEntries(
      (g?.adjustments ?? []).map((a) => [a.studentId, { delta: a.delta, reason: a.reason }])
    )
  };
  for (const c of screen.criteria) {
    const s = g?.scores.find((x) => x.criterionId === c.id);
    form.scores[c.id] = s?.score ?? null;
    form.comments[c.id] = s?.comment ?? '';
    form.levels[c.id] = s?.levelId ?? null;
  }
  for (const m of g?.checklist ?? []) form.checked[m.checklistItemId] = m.checked;
  return form;
}

function toRequest(screen: GradingScreen, form: GradeForm): SaveGradeRequest {
  const paper = isPaper(screen);
  return {
    versionNo: screen.version.versionNo,
    // Chấm 0 (BR-19): server đặt mọi tiêu chí = 0, gửi điểm khác 0 sẽ bị từ chối.
    scores: screen.criteria.map((c) => ({
      criterionId: c.id,
      score: form.zeroReason ? 0 : form.scores[c.id],
      comment: form.comments[c.id]?.trim() || null,
      ...(paper ? { levelId: form.zeroReason ? null : form.levels[c.id] } : {})
    })),
    checklist: Object.entries(form.checked)
      .filter(([, checked]) => checked)
      .map(([id]) => ({ checklistItemId: Number(id), checked: true })),
    inlineComments: [...form.inline]
      .sort((a, b) => a.startOffset - b.startOffset)
      .map((ic) => ({
        startOffset: ic.startOffset,
        endOffset: ic.endOffset,
        quote: ic.quote,
        kind: ic.kind,
        errorCode: ic.errorCode,
        criterionId: ic.criterionId,
        comment: ic.comment,
        suggestion: ic.suggestion,
        edit: ic.edit
      })),
    generalComment: form.generalComment.trim() || null,
    zeroReason: form.zeroReason,
    ...(paper
      ? {
          memberAdjustments: screen.members
            .filter((m) => readyAdjustment(form.adjustments[m.id]))
            .map((m) => ({
              studentId: m.id,
              delta: form.adjustments[m.id].delta,
              reason: form.adjustments[m.id].reason.trim()
            }))
        }
      : {})
  };
}

/**
 * Giới hạn điểm hiện tại của một tiêu chí theo checklist "Giới hạn điểm" đang tick và số từ. Bài PAPER không áp giới
 * hạn bài ≤ 20 từ (BR-18).
 */
export function capOf(c: GradingCriterion, checked: Record<number, boolean>, wordCount: number, paper = false) {
  const limits = c.checklist.filter((i) => i.kind === 'LIMIT' && checked[i.id]);
  return scoreCap(c.maxScore, limits, paper ? Number.POSITIVE_INFINITY : wordCount);
}

/** Mức đang chọn vẫn chứa điểm thì giữ; không thì đổi sang mức chứa điểm (hoặc bỏ mức) — tránh lỗi LEVEL_MISMATCH. */
function syncLevel(levels: LevelDto[], levelId: number | null, score: number | null): number | null {
  if (score === null) return levelId;
  const current = levels.find((l) => l.id === levelId);
  if (current && levelOfScore([current], score)) return levelId;
  return levelOfScore(levels, score)?.id ?? null;
}

export function useGradeForm(screen: GradingScreen) {
  const [form, setForm] = useState<GradeForm>(() => initForm(screen));
  const [grade, setGrade] = useState<GradeDto | null>(screen.grade);
  const [state, setState] = useState<GradeSaveState>('SAVED');
  const [error, setError] = useState<string | null>(null);

  const formRef = useRef(form);
  const savedJsonRef = useRef(JSON.stringify(toRequest(screen, initForm(screen))));
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inflightRef = useRef<Promise<boolean> | null>(null);
  const saveRef = useRef<() => Promise<boolean>>(() => Promise.resolve(true));
  const wordCount = screen.version.wordCount;
  const editable = screen.editable;
  const paper = isPaper(screen);

  const save = useCallback(async (): Promise<boolean> => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = null;
    while (inflightRef.current) await inflightRef.current;
    const body = toRequest(screen, formRef.current);
    const json = JSON.stringify(body);
    if (json === savedJsonRef.current) {
      setState('SAVED');
      return true;
    }
    setState('SAVING');
    const request = gradingApi
      .saveGrade(screen.submission.id, body)
      .then((saved) => {
        savedJsonRef.current = json;
        setGrade(saved);
        setError(null);
        if (JSON.stringify(toRequest(screen, formRef.current)) !== json) {
          setState('DIRTY');
          timerRef.current = setTimeout(() => void saveRef.current(), DEBOUNCE_MS);
        } else {
          setState('SAVED');
        }
        return true;
      })
      .catch((err: unknown) => {
        setError(getErrorMessage(err, 'Không lưu được kết quả chấm'));
        setState('ERROR');
        return false;
      })
      .finally(() => {
        inflightRef.current = null;
      });
    inflightRef.current = request;
    return request;
  }, [screen]);
  useEffect(() => {
    saveRef.current = save;
  }, [save]);

  const change = useCallback(
    (updater: (f: GradeForm) => GradeForm) => {
      if (!editable) return;
      const next = updater(formRef.current);
      formRef.current = next;
      setForm(next);
      setState((s) => (s === 'SAVING' ? s : 'DIRTY'));
      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = setTimeout(() => void saveRef.current(), DEBOUNCE_MS);
    },
    [editable]
  );

  const setScore = useCallback(
    (criterion: GradingCriterion, score: number | null) =>
      change((f) => ({
        ...f,
        scores: { ...f.scores, [criterion.id]: score },
        levels: { ...f.levels, [criterion.id]: syncLevel(criterion.levels, f.levels[criterion.id], score) }
      })),
    [change]
  );

  /** Bài PAPER: bấm một mức → điểm nhảy vào khoảng của mức (giữ điểm nếu đã nằm trong khoảng); bấm lại để bỏ. */
  const setLevel = useCallback(
    (criterion: GradingCriterion, level: LevelDto | null) =>
      change((f) => {
        if (!level) return { ...f, levels: { ...f.levels, [criterion.id]: null } };
        const cap = capOf(criterion, f.checked, wordCount, true);
        const score = clampScore(scoreForLevel(level, f.scores[criterion.id]), cap.max);
        return {
          ...f,
          levels: { ...f.levels, [criterion.id]: syncLevel(criterion.levels, level.id, score) },
          scores: { ...f.scores, [criterion.id]: score }
        };
      }),
    [change, wordCount]
  );

  const setComment = useCallback(
    (criterionId: number, comment: string) =>
      change((f) => ({ ...f, comments: { ...f.comments, [criterionId]: comment } })),
    [change]
  );

  /** Tick / bỏ tick một mục; tick "Giới hạn điểm" làm điểm vượt mức → hạ xuống đúng mức (BR-17). */
  const toggleItem = useCallback(
    (criterion: GradingCriterion, itemId: number, value: boolean) =>
      change((f) => {
        const checked = { ...f.checked, [itemId]: value };
        const cap = capOf(criterion, checked, wordCount, paper);
        const score = clampScore(f.scores[criterion.id], cap.max);
        return {
          ...f,
          checked,
          scores: { ...f.scores, [criterion.id]: score },
          levels: { ...f.levels, [criterion.id]: syncLevel(criterion.levels, f.levels[criterion.id], score) }
        };
      }),
    [change, wordCount, paper]
  );

  const keySeq = useRef(0);

  /** Thêm một đánh dấu, trả về key để chọn ngay. */
  const addInline = useCallback(
    (draft: InlineDraft) => {
      keySeq.current += 1;
      const key = `n${keySeq.current}`;
      change((f) => ({ ...f, inline: [...f.inline, { ...draft, key }] }));
      return key;
    },
    [change]
  );

  const updateInline = useCallback(
    (key: string, draft: InlineDraft) =>
      change((f) => ({ ...f, inline: f.inline.map((x) => (x.key === key ? { ...draft, key } : x)) })),
    [change]
  );

  const removeInline = useCallback(
    (key: string) => change((f) => ({ ...f, inline: f.inline.filter((x) => x.key !== key) })),
    [change]
  );

  const setGeneralComment = useCallback((v: string) => change((f) => ({ ...f, generalComment: v })), [change]);
  const setZeroReason = useCallback((r: ZeroReason | null) => change((f) => ({ ...f, zeroReason: r })), [change]);

  /** Bài PAPER: đặt / bỏ (`delta = null`) cộng / trừ của một thành viên; giữ lý do đã gõ khi đổi mức. */
  const setAdjustment = useCallback(
    (studentId: number, delta: number | null) =>
      change((f) => {
        const adjustments = { ...f.adjustments };
        if (delta === null) delete adjustments[studentId];
        else adjustments[studentId] = { delta, reason: f.adjustments[studentId]?.reason ?? '' };
        return { ...f, adjustments };
      }),
    [change]
  );

  const setAdjustmentReason = useCallback(
    (studentId: number, reason: string) =>
      change((f) =>
        f.adjustments[studentId]
          ? { ...f, adjustments: { ...f.adjustments, [studentId]: { ...f.adjustments[studentId], reason } } }
          : f
      ),
    [change]
  );

  /** Lưu ngay những gì chưa lưu (trước khi trả bài / yêu cầu sửa / rời màn). */
  const flush = useCallback(() => save(), [save]);

  // Rời màn khi còn thay đổi → lưu nốt; đóng trang khi chưa lưu → trình duyệt hỏi lại.
  useEffect(() => {
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      if (JSON.stringify(toRequest(screen, formRef.current)) !== savedJsonRef.current) e.preventDefault();
    };
    window.addEventListener('beforeunload', onBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', onBeforeUnload);
      if (timerRef.current) {
        clearTimeout(timerRef.current);
        void saveRef.current();
      }
    };
  }, [screen]);

  const allScored = form.zeroReason !== null || screen.criteria.every((c) => form.scores[c.id] !== null);
  const bandPreview = useMemo(() => {
    if (form.zeroReason) return 0;
    const raw = partBandRaw(screen.criteria.map((c) => ({ score: form.scores[c.id], weight: c.weight })));
    return paper ? paperScore(raw) : roundBand(raw);
  }, [form, screen.criteria, paper]);

  return {
    form,
    grade,
    state,
    error,
    allScored,
    bandPreview,
    paper,
    setScore,
    setLevel,
    setComment,
    toggleItem,
    setGeneralComment,
    setZeroReason,
    setAdjustment,
    setAdjustmentReason,
    addInline,
    updateInline,
    removeInline,
    save,
    flush
  };
}
