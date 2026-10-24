'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

import { studentApi } from '@/lib/api/student';
import { ApiError, getErrorMessage } from '@/lib/apiError';
import type { DraftConflict } from '@/lib/types/student';

// ─── Tự lưu nháp (docs/06 mục 7, SD-06) ───────────────────────────────────────
// Gõ → DIRTY; ngừng gõ 3 giây (hoặc rời tab) → SAVING → SAVED. Server trả 409 DRAFT_CONFLICT
// (tab / máy khác vừa lưu) → CONFLICT, ngừng tự lưu tới khi sinh viên chọn bản. Gửi `draftVersion`
// đang có để server phát hiện xung đột. Mất mạng / nháp offline: bước F4c.

export type SaveState = 'SAVED' | 'DIRTY' | 'SAVING' | 'ERROR' | 'CONFLICT';

const DEBOUNCE_MS = 3_000;

/** Lỗi nghĩa là phần này không còn soạn được nữa (đã nộp ở nơi khác, quá hạn, bài đóng) → trang tải lại. */
const FATAL_CODES = new Set(['ALREADY_SUBMITTED', 'DEADLINE_PASSED', 'INVALID_STATE']);

export interface DraftInit {
  content: string;
  draftVersion: number;
  savedAt: string | null;
}

export function useDraftAutosave(partId: number, init: DraftInit, onFatal: () => void) {
  const [content, setContentState] = useState(init.content);
  const [state, setState] = useState<SaveState>('SAVED');
  const [savedAt, setSavedAt] = useState(init.savedAt);
  const [error, setError] = useState<string | null>(null);
  const [conflict, setConflict] = useState<DraftConflict | null>(null);

  const contentRef = useRef(init.content);
  /** Nội dung server đang giữ (lần lưu thành công gần nhất). */
  const savedContentRef = useRef(init.content);
  const versionRef = useRef(init.draftVersion);
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

  const clearTimer = useCallback(() => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = null;
  }, []);

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
    setState('SAVING');
    const request = studentApi
      .saveDraft(partId, text, versionRef.current)
      .then((res) => {
        versionRef.current = res.draftVersion;
        savedContentRef.current = text;
        setSavedAt(res.savedAt);
        setError(null);
        // Gõ thêm trong lúc đang lưu → còn thay đổi chưa lưu, hẹn lưu tiếp.
        if (contentRef.current !== text) {
          setState('DIRTY');
          timerRef.current = setTimeout(() => void saveRef.current(), DEBOUNCE_MS);
        } else {
          setState('SAVED');
        }
        return true;
      })
      .catch((err: unknown) => {
        if (err instanceof ApiError && err.code === 'DRAFT_CONFLICT') {
          conflictRef.current = true;
          setConflict(err.data as DraftConflict);
          setState('CONFLICT');
        } else {
          setError(getErrorMessage(err, 'Không lưu được bản nháp'));
          setState('ERROR');
          if (err instanceof ApiError && err.code && FATAL_CODES.has(err.code)) {
            stoppedRef.current = true;
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
  }, [partId, clearTimer]);
  useEffect(() => {
    saveRef.current = save;
  }, [save]);

  const setContent = useCallback(
    (text: string) => {
      contentRef.current = text;
      setContentState(text);
      if (conflictRef.current || stoppedRef.current) return;
      setState((s) => (s === 'SAVING' ? s : 'DIRTY'));
      clearTimer();
      timerRef.current = setTimeout(() => void save(), DEBOUNCE_MS);
    },
    [clearTimer, save]
  );

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
      } else {
        void save();
      }
    },
    [conflict, save]
  );

  /** Gọi trước khi nộp: huỷ lần tự lưu đang hẹn và đợi lần đang chạy xong. */
  const pause = useCallback(async () => {
    clearTimer();
    while (inflightRef.current) await inflightRef.current;
  }, [clearTimer]);

  /** Nộp xong: không tự lưu nữa (server đã xoá nháp, lưu tiếp sẽ bị từ chối). */
  const stop = useCallback(() => {
    stoppedRef.current = true;
    clearTimer();
  }, [clearTimer]);

  // Rời tab → lưu ngay; đóng trang khi còn thay đổi chưa lưu → trình duyệt hỏi lại.
  useEffect(() => {
    const onHidden = () => {
      if (document.visibilityState === 'hidden' && contentRef.current !== savedContentRef.current) void save();
    };
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      if (!stoppedRef.current && contentRef.current !== savedContentRef.current) e.preventDefault();
    };
    document.addEventListener('visibilitychange', onHidden);
    window.addEventListener('beforeunload', onBeforeUnload);
    return () => {
      document.removeEventListener('visibilitychange', onHidden);
      window.removeEventListener('beforeunload', onBeforeUnload);
    };
  }, [save]);

  // Rời màn (đổi phần, quay lại) khi còn thay đổi → lưu nốt.
  useEffect(
    () => () => {
      if (timerRef.current && !stoppedRef.current && !conflictRef.current) {
        clearTimeout(timerRef.current);
        void save();
      }
    },
    [save]
  );

  return { content, setContent, state, savedAt, error, conflict, save, resolveConflict, pause, stop };
}
