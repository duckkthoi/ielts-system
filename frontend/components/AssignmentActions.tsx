'use client';

import { useCallback } from 'react';

import { App } from 'antd';
import type { MenuProps } from 'antd';
import { Copy, Lock, LockOpen, Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';

import { assignmentApi } from '@/lib/api/assignment';
import type { AssignmentStatus } from '@/lib/theme/status';

// ─── Thao tác trên một bài tập (danh sách + chi tiết) ─────────────────────────
// Sửa nội dung: chỉ bài nháp (BR-06). Xoá: chỉ bài nháp. Đóng / mở lại: bài đã phát hành.

export const ASSIGNMENT_ACTION = {
  edit: 'edit',
  duplicate: 'duplicate',
  close: 'close',
  reopen: 'reopen',
  remove: 'remove'
} as const;

export const editAssignmentPath = (id: number) => `/giao-vien/bai-tap/tao-moi?id=${id}`;
export const assignmentPath = (id: number) => `/giao-vien/bai-tap/${id}`;

interface Target {
  id: number;
  title: string;
  status: AssignmentStatus;
}

/** `onChanged`: sau đóng / mở lại; `onRemoved`: sau xoá (mặc định cũng gọi onChanged). */
export function useAssignmentActions(onChanged: () => void, onRemoved?: () => void) {
  const router = useRouter();
  const { modal, message } = App.useApp();

  const menuItems = useCallback(
    (a: Target): MenuProps['items'] => [
      ...(a.status === 'DRAFT'
        ? [{ key: ASSIGNMENT_ACTION.edit, icon: <Pencil size={14} />, label: 'Sửa nội dung' }]
        : []),
      { key: ASSIGNMENT_ACTION.duplicate, icon: <Copy size={14} />, label: 'Nhân bản' },
      ...(a.status === 'PUBLISHED'
        ? [{ key: ASSIGNMENT_ACTION.close, icon: <Lock size={14} />, label: 'Đóng bài' }]
        : []),
      ...(a.status === 'CLOSED'
        ? [{ key: ASSIGNMENT_ACTION.reopen, icon: <LockOpen size={14} />, label: 'Mở lại' }]
        : []),
      ...(a.status === 'DRAFT'
        ? [
            { type: 'divider' as const },
            { key: ASSIGNMENT_ACTION.remove, icon: <Trash2 size={14} />, label: 'Xoá', danger: true }
          ]
        : [])
    ],
    []
  );

  const run = useCallback(
    (a: Target, key: string) => {
      if (key === ASSIGNMENT_ACTION.edit) router.push(editAssignmentPath(a.id));
      if (key === ASSIGNMENT_ACTION.duplicate) {
        assignmentApi
          .duplicate(a.id)
          .then((res) => {
            message.success(res.message ?? 'Đã tạo bản sao (nháp)');
            router.push(assignmentPath(res.data.id));
          })
          .catch(() => undefined);
      }
      if (key === ASSIGNMENT_ACTION.close) {
        modal.confirm({
          title: `Đóng bài "${a.title}"?`,
          content: 'Sinh viên không nộp hay sửa bài được nữa; bạn vẫn chấm và trả bài bình thường. Có thể mở lại sau.',
          okText: 'Đóng bài',
          cancelText: 'Huỷ',
          onOk: async () => {
            const res = await assignmentApi.close(a.id);
            message.success(res.message ?? 'Đã đóng bài tập');
            onChanged();
          }
        });
      }
      if (key === ASSIGNMENT_ACTION.reopen) {
        assignmentApi
          .reopen(a.id)
          .then((res) => {
            message.success(res.message ?? 'Đã mở lại bài tập');
            onChanged();
          })
          .catch(() => undefined);
      }
      if (key === ASSIGNMENT_ACTION.remove) {
        modal.confirm({
          title: `Xoá bài nháp "${a.title}"?`,
          content: 'Bài nháp và toàn bộ đề, rubric sẽ bị xoá. Không thể hoàn tác.',
          okText: 'Xoá',
          okButtonProps: { danger: true },
          cancelText: 'Huỷ',
          onOk: async () => {
            const res = await assignmentApi.remove(a.id);
            message.success(res.message ?? 'Đã xoá bài tập');
            (onRemoved ?? onChanged)();
          }
        });
      }
    },
    [router, message, modal, onChanged, onRemoved]
  );

  return { menuItems, run };
}
