'use client';

import { useCallback, useMemo } from 'react';

import { Avatar, Dropdown } from 'antd';
import type { MenuProps } from 'antd';
import { ChevronDown, KeyRound, LogOut, UserRound } from 'lucide-react';
import { useRouter } from 'next/navigation';

import { ConnectionStatus } from '@/components/ConnectionStatus';
import { NotificationBell } from '@/components/NotificationBell';
import { ROLE_LABEL, ROUTES } from '@/lib/constants';
import { useAuthStore } from '@/stores/useAuthStore';

// ─── Nhóm nút bên phải header: trạng thái kết nối, chuông thông báo, menu tài khoản ─

const USER_MENU_KEY = {
  profile: 'profile',
  changePassword: 'change-password',
  logout: 'logout'
} as const;

/** Chữ cái đầu của tên (từ cuối trong họ tên tiếng Việt) để hiện trên avatar. */
function getInitial(fullName: string): string {
  return fullName.trim().split(/\s+/).at(-1)?.charAt(0).toUpperCase() ?? '';
}

export function AppHeaderActions() {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  const menuItems = useMemo<MenuProps['items']>(
    () => [
      { key: USER_MENU_KEY.profile, icon: <UserRound size={14} />, label: 'Thông tin cá nhân' },
      { key: USER_MENU_KEY.changePassword, icon: <KeyRound size={14} />, label: 'Đổi mật khẩu' },
      { type: 'divider' },
      { key: USER_MENU_KEY.logout, icon: <LogOut size={14} />, label: 'Đăng xuất', danger: true }
    ],
    []
  );

  const handleMenuClick = useCallback<NonNullable<MenuProps['onClick']>>(
    async ({ key }) => {
      if (key === USER_MENU_KEY.profile) router.push('/ca-nhan');
      if (key === USER_MENU_KEY.changePassword) router.push(ROUTES.changePassword);
      if (key === USER_MENU_KEY.logout) {
        await logout();
        router.replace(ROUTES.login);
      }
    },
    [router, logout]
  );

  const menu = useMemo(() => ({ items: menuItems, onClick: handleMenuClick }), [menuItems, handleMenuClick]);

  if (!user) return null;

  return (
    <div className="flex shrink-0 items-center gap-1 sm:gap-2">
      <ConnectionStatus />
      <NotificationBell />
      <Dropdown menu={menu} trigger={['click']} placement="bottomRight">
        <button
          type="button"
          aria-label="Tài khoản người dùng"
          className="flex cursor-pointer items-center gap-2.5 rounded-lg px-2.5 py-1.5 transition-colors hover:bg-neutral-50 focus:outline-none"
        >
          <Avatar className="bg-brand-50 font-medium text-brand-700">{getInitial(user.fullName)}</Avatar>
          <span className="hidden text-left leading-[18px] sm:grid">
            <span className="font-semibold text-neutral-900">{user.fullName}</span>
            <span className="text-caption text-neutral-500">{ROLE_LABEL[user.role]}</span>
          </span>
          <ChevronDown size={14} className="text-neutral-400" />
        </button>
      </Dropdown>
    </div>
  );
}
