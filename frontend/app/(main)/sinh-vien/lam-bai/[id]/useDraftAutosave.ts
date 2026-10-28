'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

import { studentApi } from '@/lib/api/student';
import { ApiError, getErrorMessage } from '@/lib/apiError';
import { offlineDraftStore } from '@/lib/offlineDraftStore';
import type { DraftConflict } from '@/lib/types/student';
import { decideRestore, isNetworkFailure, offlineDraftKey } from '@/lib/utils/offlineDraft';

// ─── Tự lưu nháp + nháp offline (docs/06 mục 7, SD-06) ────────────────────────
// Gõ → DIRTY (và ghi ngay vào IndexedDB); ngừng gõ 3 giây (hoặc rời tab) → SAVING → SAVED.
// Mất mạng → OFFLINE ("đã lưu trên máy"), trình duyệt báo `online` → gửi lại. Server trả 409 DRAFT_CONFLICT
// (tab / máy khác vừa lưu) → CONFLICT, ngừng tự lưu tới khi sinh viên chọn bản. Mở lại màn mà trên máy còn
// bản chưa gửi được → server chưa đổi thì gửi lên, server đã đổi thì hỏi chọn bản.

export type SaveState = 'SAVED' | 'DIRTY' | 'SAVING' | 'OFFLINE' | 'ERROR' | 'CONFLICT';

const DEBOUNCE_MS = 3_000;

/** Lỗi nghĩa là phần này không còn soạn được nữa (đã nộp ở nơi khác, quá hạn, bài đóng) → trang tải lại. */
const FATAL_CODES = new Set(['ALREADY_SUBMITTED', 'DEADLINE_PASSED', 'INVALID_STATE']);

export interface DraftInit {
  content: string;
  draftVersion: number;
  savedAt: string | null;
  /** Bài làm trên server (kênh chat bình luận trực tiếp); chưa lưu lần nào → null. */
  submissionId: number | null;
}

export function useDraftAutosave(userId: number, partId: number, init: DraftInit, onFatal: () => void) {
  const [content, setContentState] = useState(init.content);
  const [state, setState] = useState<SaveState>('SAVED');
  const [savedAt, setSavedAt] = useState(init.savedAt);
  const [error, setError] = useState<string | null>(null);
  const [conflict, setConflict] = useState<DraftConflict | null>(null);
  const [submissionId, setSubmissionId] = useState(init.submissionId);

  const localKey = offlineDraftKey(userId, partId);
  const contentRef = useRef(init.content);
  /** Nội dung server đang giữ (lần lưu thành công gần nhất). */
  const savedContentRef = useRef(init.content);
  const versionRef = useRef(init.draftVersion);
  const stateRef = useRef<SaveState>('SAVED');
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inflightRef = useRef<Promise<boolean> | null>(null);
  const conflictRef = useRef(false);
  const stoppedRef = useRef(false);
  /** Hẹn lưu lại từ bên trong `save` (gọi qua ref để không tự tham chiếu). */
  const saveRef = useRef<() => Promise<boolean>>(() => Promise.resolve(true));
  const onFatalRef = useRef(onFatal);
  useEffect(() => {
    onFatalRef.current = onFatal;
  }, [onFatal]);
  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  const clearTimer = useCallback(() => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = null;
  }, []);

  /** Ghi bản đang soạn vào IndexedDB kèm `draftVersion` của server mà nó dựa trên. */
  const keepLocal = useCallback(
    (text: string) =>
      void offlineDraftStore.put(localKey, {
        content: text,
        baseDraftVersion: versionRef.current,
        updatedAt: Date.now()
      }),
    [localKey]
  );

  const save = useCallback(async (): Promise<boolean> => {
    clearTimer();
    // Đợi lần lưu đang chạy xong rồi mới quyết định (tránh 2 request cùng draftVersion).
    while (inflightRef.current) await inflightRef.current;
    if (stoppedRef.current || conflictRef.current) return !conflictRef.current;
    const text = contentRef.current;
    if (text === savedContentRef.current) {
      setState('SAVED');
      return true;
    }
    if (!navigator.onLine) {
      setState('OFFLINE');
      return false;
    }
    setState('SAVING');
    const request = studentApi
      .saveDraft(partId, text, versionRef.current)
      .then((res) => {
        versionRef.current = res.draftVersion;
        savedContentRef.current = text;
        setSavedAt(res.savedAt);
        setSubmissionId(res.submissionId);
        setError(null);
        // Gõ thêm trong lúc đang lưu → còn thay đổi chưa lưu, hẹn lưu tiếp.
        if (contentRef.current !== text) {
          keepLocal(contentRef.current);
          setState('DIRTY');
          timerRef.current = setTimeout(() => void saveRef.current(), DEBOUNCE_MS);
        } else {
          void offlineDraftStore.remove(localKey);
          setState('SAVED');
        }
        return true;
      })
      .catch((err: unknown) => {
        if (err instanceof ApiError && err.code === 'DRAFT_CONFLICT') {
          conflictRef.current = true;
          setConflict(err.data as DraftConflict);
          setState('CONFLICT');
        } else if (err instanceof ApiError && isNetworkFailure(err.status)) {
          // Bản đang soạn đã nằm trong IndexedDB; có mạng lại sẽ gửi tiếp.
          setState('OFFLINE');
        } else {
          setError(getErrorMessage(err, 'Không lưu được bản nháp'));
          setState('ERROR');
          if (err instanceof ApiError && err.code && FATAL_CODES.has(err.code)) {
            stoppedRef.current = true;
            void offlineDraftStore.remove(localKey);
            onFatalRef.current();
          }
        }
        return false;
      })
      .finally(() => {
        inflightRef.current = null;
      });
    inflightRef.current = request;
    return request;
  }, [partId, localKey, clearTimer, keepLocal]);
  useEffect(() => {
    saveRef.current = save;
  }, [save]);

  const setContent = useCallback(
    (text: string) => {
      contentRef.current = text;
      setContentState(text);
      if (stoppedRef.current) return;
      keepLocal(text);
      if (conflictRef.current) return;
      setState((s) => (s === 'SAVING' || s === 'OFFLINE' ? s : 'DIRTY'));
      clearTimer();
      timerRef.current = setTimeout(() => void save(), DEBOUNCE_MS);
    },
    [clearTimer, keepLocal, save]
  );

  // Mở màn: trên máy còn bản chưa gửi được thì khôi phục (hoặc hỏi chọn nếu server cũng đã đổi).
  useEffect(() => {
    let cancelled = false;
    void offlineDraftStore.get(localKey).then((local) => {
      if (cancelled || !local) return;
      const decision = decideRestore(local, { content: init.content, draftVersion: init.draftVersion });
      if (decision === 'restore') {
        setContent(local.content);
      } else if (decision === 'conflict') {
        contentRef.current = local.content;
        setContentState(local.content);
        conflictRef.current = true;
        setConflict({ serverContent: init.content, draftVersion: init.draftVersion, savedAt: init.savedAt });
        setState('CONFLICT');
      } else {
        void offlineDraftStore.remove(localKey);
      }
    });
    return () => {
      cancelled = true;
    };
    // Chỉ chạy một lần khi mở phần này (panel được dựng lại khi đổi phần / trạng thái).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** Sinh viên chọn bản: giữ bản trên máy này (ghi đè server) hoặc lấy bản trên server. */
  const resolveConflict = useCallback(
    (choice: 'local' | 'server') => {
      if (!conflict) return;
      const serverContent = conflict.serverContent ?? '';
      versionRef.current = conflict.draftVersion;
      savedContentRef.current = serverContent;
      conflictRef.current = false;
      setConflict(null);
      if (choice === 'server') {
        contentRef.current = serverContent;
        setContentState(serverContent);
        setSavedAt(conflict.savedAt);
        setState('SAVED');
        void offlineDraftStore.remove(localKey);
      } else {
        keepLocal(contentRef.current);
        void save();
      }
    },
    [conflict, localKey, keepLocal, save]
  );

  /** Gọi trước khi nộp: huỷ lần tự lưu đang hẹn và đợi lần đang chạy xong. */
  const pause = useCallback(async () => {
    clearTimer();
    while (inflightRef.current) await inflightRef.current;
  }, [clearTimer]);

  /** Nộp xong: không tự lưu nữa (server đã xoá nháp, lưu tiếp sẽ bị từ chối) và bỏ bản trên máy. */
  const stop = useCallback(() => {
    stoppedRef.current = true;
    clearTimer();
    void offlineDraftStore.remove(localKey);
  }, [clearTimer, localKey]);

  // Rời tab → lưu ngay; có mạng lại → gửi bản đang giữ trên máy; đóng trang khi còn thay đổi chưa lưu → hỏi lại.
  useEffect(() => {
    const onHidden = () => {
      if (document.visibilityState === 'hidden' && contentRef.current !== savedContentRef.current) void save();
    };
    const onOnline = () => {
      if (stateRef.current === 'OFFLINE') void save();
    };
    const onOffline = () => {
      if (contentRef.current !== savedContentRef.current && !conflictRef.current) setState('OFFLINE');
    };
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      if (!stoppedRef.current && contentRef.current !== savedContentRef.current) e.preventDefault();
    };
    document.addEventListener('visibilitychange', onHidden);
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
    window.addEventListener('beforeunload', onBeforeUnload);
    return () => {
      document.removeEventListener('visibilitychange', onHidden);
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
      window.removeEventListener('beforeunload', onBeforeUnload);
    };
  }, [save]);

  // Rời màn (đổi phần, quay lại) khi còn thay đổi → lưu nốt (bản trên máy vẫn còn nếu lưu thất bại).
  useEffect(
    () => () => {
      if (timerRef.current && !stoppedRef.current && !conflictRef.current) {
        clearTimeout(timerRef.current);
        void save();
      }
    },
    [save]
  );

  return { content, setContent, state, savedAt, error, conflict, submissionId, save, resolveConflict, pause, stop };
}
