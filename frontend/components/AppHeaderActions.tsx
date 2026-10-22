'use client';

import { useCallback, useMemo } from 'react';

import { Badge, Button, Dropdown } from 'antd';
import type { MenuProps } from 'antd';
import { Bell, KeyRound, LogOut, UserRound } from 'lucide-react';
import { useRouter } from 'next/navigation';

import { IconButton } from '@/components/IconButton';
import { ROLE_LABEL, ROUTES } from '@/lib/constants';
import { useAuthStore } from '@/stores/useAuthStore';

// ─── Nhóm nút bên phải header: thông báo + menu tài khoản ─────────────────────
// Số thông báo chưa đọc sẽ nối với WebSocket ở bước thông báo realtime.

const USER_MENU_KEY = {
  profile: 'profile',
  changePassword: 'change-password',
  logout: 'logout'
} as const;

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

  const goNotifications = useCallback(() => router.push('/thong-bao'), [router]);

  if (!user) return null;

  return (
    <div className="flex items-center gap-2">
      <Badge count={0} size="small">
        <IconButton title="Thông báo" type="text" icon={<Bell size={18} />} onClick={goNotifications} />
      </Badge>
      <Dropdown menu={menu} trigger={['click']}>
        <Button type="text" className="flex items-center gap-2">
          <span className="font-medium">{user.fullName}</span>
          <span className="text-xs opacity-70">{ROLE_LABEL[user.role]}</span>
        </Button>
      </Dropdown>
    </div>
  );
}
