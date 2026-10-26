'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { gradingApi } from '@/lib/api/grading';
import { getErrorMessage } from '@/lib/apiError';
import type { GradeDto, GradingCriterion, GradingScreen, SaveGradeRequest, ZeroReason } from '@/lib/types/grading';
import { clampScore, partBandRaw, roundBand, scoreCap } from '@/lib/utils/grading';

// ─── Form chấm một version (SD-08) ────────────────────────────────────────────
// Giữ điểm / checklist / nhận xét trên client, tự lưu nháp sau 2 giây ngừng sửa (PUT …/grade thay toàn bộ).
// Giới hạn điểm và band chỉ tính để XEM TRƯỚC (BR-12, BR-17, BR-18); server tính lại khi lưu.
// Nhận xét inline hiện có được gửi lại nguyên vẹn để không bị xoá (sửa nhận xét inline: bước F5b).

export type GradeSaveState = 'SAVED' | 'DIRTY' | 'SAVING' | 'ERROR';

const DEBOUNCE_MS = 2_000;

export interface GradeForm {
  scores: Record<number, number | null>;
  comments: Record<number, string>;
  checked: Record<number, boolean>;
  generalComment: string;
  zeroReason: ZeroReason | null;
}

function initForm(screen: GradingScreen): GradeForm {
  const g = screen.grade;
  const form: GradeForm = {
    scores: {},
    comments: {},
    checked: {},
    generalComment: g?.generalComment ?? '',
    zeroReason: g?.zeroReason ?? null
  };
  for (const c of screen.criteria) {
    const s = g?.scores.find((x) => x.criterionId === c.id);
    form.scores[c.id] = s?.score ?? null;
    form.comments[c.id] = s?.comment ?? '';
  }
  for (const m of g?.checklist ?? []) form.checked[m.checklistItemId] = m.checked;
  return form;
}

function toRequest(screen: GradingScreen, form: GradeForm): SaveGradeRequest {
  return {
    versionNo: screen.version.versionNo,
    // Chấm 0 (BR-19): server đặt mọi tiêu chí = 0, gửi điểm khác 0 sẽ bị từ chối.
    scores: screen.criteria.map((c) => ({
      criterionId: c.id,
      score: form.zeroReason ? 0 : form.scores[c.id],
      comment: form.comments[c.id]?.trim() || null
    })),
    checklist: Object.entries(form.checked)
      .filter(([, checked]) => checked)
      .map(([id]) => ({ checklistItemId: Number(id), checked: true })),
    inlineComments: (screen.grade?.inlineComments ?? []).map((ic) => ({
      startOffset: ic.startOffset,
      endOffset: ic.endOffset,
      quote: ic.quote,
      comment: ic.comment,
      errorCode: ic.errorCode,
      criterionId: ic.criterionId,
      suggestion: ic.suggestion
    })),
    generalComment: form.generalComment.trim() || null,
    zeroReason: form.zeroReason
  };
}

/** Giới hạn điểm hiện tại của một tiêu chí theo checklist "Giới hạn điểm" đang tick và số từ. */
export function capOf(c: GradingCriterion, checked: Record<number, boolean>, wordCount: number) {
  const limits = c.checklist.filter((i) => i.kind === 'LIMIT' && checked[i.id]);
  return scoreCap(c.maxScore, limits, wordCount);
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
    (criterionId: number, score: number | null) =>
      change((f) => ({ ...f, scores: { ...f.scores, [criterionId]: score } })),
    [change]
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
        const cap = capOf(criterion, checked, wordCount);
        return { ...f, checked, scores: { ...f.scores, [criterion.id]: clampScore(f.scores[criterion.id], cap.max) } };
      }),
    [change, wordCount]
  );

  const setGeneralComment = useCallback((v: string) => change((f) => ({ ...f, generalComment: v })), [change]);
  const setZeroReason = useCallback((r: ZeroReason | null) => change((f) => ({ ...f, zeroReason: r })), [change]);

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
    return roundBand(partBandRaw(screen.criteria.map((c) => ({ score: form.scores[c.id], weight: c.weight }))));
  }, [form, screen.criteria]);

  return {
    form,
    grade,
    state,
    error,
    allScored,
    bandPreview,
    setScore,
    setComment,
    toggleItem,
    setGeneralComment,
    setZeroReason,
    save,
    flush
  };
}
