'use client';

import { useCallback, useState } from 'react';

import { App, Button, Dropdown } from 'antd';
import type { MenuProps } from 'antd';
import { KeyRound, Lock, LockOpen, MoreHorizontal, Pencil } from 'lucide-react';

import { TemporaryPasswordModal } from '@/components/TemporaryPasswordModal';
import type { TemporaryPasswordInfo } from '@/components/TemporaryPasswordModal';
import { adminApi } from '@/lib/api/admin';
import type { UserSummary } from '@/lib/types/admin';
import { useAuthStore } from '@/stores/useAuthStore';

// ─── Thao tác trên một tài khoản (trang Giáo viên + Người dùng) ───────────────
// Khoá / mở khoá / đặt lại mật khẩu có hộp thoại xác nhận; đặt lại xong hiện mật khẩu tạm một lần.

const ACTION = { edit: 'edit', reset: 'reset', lock: 'lock', unlock: 'unlock' } as const;

export function useAccountActions(onChanged: () => void) {
  const { modal, message } = App.useApp();
  const myId = useAuthStore((s) => s.user?.id);
  const [password, setPassword] = useState<TemporaryPasswordInfo | null>(null);

  const confirmReset = useCallback(
    (user: UserSummary) =>
      modal.confirm({
        title: `Đặt lại mật khẩu cho ${user.fullName}?`,
        content: 'Mọi phiên đăng nhập của tài khoản này sẽ bị đăng xuất; lần đăng nhập sau phải đổi mật khẩu.',
        okText: 'Đặt lại',
        cancelText: 'Huỷ',
        onOk: async () => {
          const temporaryPassword = await adminApi.resetPassword(user.id);
          setPassword({ title: 'Đã đặt lại mật khẩu', username: user.username, password: temporaryPassword });
          onChanged();
        }
      }),
    [modal, onChanged]
  );

  const confirmLock = useCallback(
    (user: UserSummary, lock: boolean) =>
      modal.confirm({
        title: lock ? `Khoá tài khoản ${user.fullName}?` : `Mở khoá tài khoản ${user.fullName}?`,
        content: lock
          ? 'Người dùng bị đăng xuất ngay trên mọi thiết bị và không đăng nhập được cho tới khi mở khoá.'
          : 'Người dùng đăng nhập lại được bằng mật khẩu hiện tại.',
        okText: lock ? 'Khoá' : 'Mở khoá',
        okButtonProps: { danger: lock },
        cancelText: 'Huỷ',
        onOk: async () => {
          const res = lock ? await adminApi.lockUser(user.id) : await adminApi.unlockUser(user.id);
          if (res.message) message.success(res.message);
          onChanged();
        }
      }),
    [modal, message, onChanged]
  );

  /** Nút "…" cuối dòng. `onEdit` có thì thêm mục "Sửa thông tin" (chỉ giáo viên sửa được qua API). */
  const renderMenu = useCallback(
    (user: UserSummary, onEdit?: (user: UserSummary) => void) => {
      const items: MenuProps['items'] = [
        ...(onEdit ? [{ key: ACTION.edit, icon: <Pencil size={14} />, label: 'Sửa thông tin' }] : []),
        { key: ACTION.reset, icon: <KeyRound size={14} />, label: 'Đặt lại mật khẩu' },
        user.active
          ? {
              key: ACTION.lock,
              icon: <Lock size={14} />,
              label: 'Khoá tài khoản',
              danger: true,
              disabled: user.id === myId
            }
          : { key: ACTION.unlock, icon: <LockOpen size={14} />, label: 'Mở khoá' }
      ];
      const onClick: MenuProps['onClick'] = ({ key, domEvent }) => {
        domEvent.stopPropagation();
        if (key === ACTION.edit) onEdit?.(user);
        if (key === ACTION.reset) confirmReset(user);
        if (key === ACTION.lock) confirmLock(user, true);
        if (key === ACTION.unlock) confirmLock(user, false);
      };
      return (
        <Dropdown menu={{ items, onClick }} trigger={['click']} placement="bottomRight">
          <Button
            type="text"
            icon={<MoreHorizontal size={16} />}
            aria-label="Thao tác"
            onClick={(e) => e.stopPropagation()}
          />
        </Dropdown>
      );
    },
    [confirmLock, confirmReset, myId]
  );

  const passwordModal = <TemporaryPasswordModal info={password} onClose={() => setPassword(null)} />;

  return { renderMenu, showPassword: setPassword, passwordModal };
}
