'use client';

import { useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Avatar, Menu, Tooltip } from 'antd';
import type { MenuProps } from 'antd';
import clsx from 'clsx';
import { LogOut } from 'lucide-react';

import { BrandMark } from '@/components/BrandMark';
import { ROLE_LABEL, ROUTES } from '@/lib/constants';
import { useAuthStore } from '@/stores/useAuthStore';

// ─── Sidebar: logo + menu theo vai trò ────────────────────────────────────────
// Dùng cho cả sidebar cố định (màn hình lg trở lên) và drawer menu trên điện thoại.
// `collapsed`: chỉ còn icon (rộng LAYOUT.siderCollapsedWidth), nhãn hiện ở tooltip.

interface AppSidebarProps {
  items: Required<MenuProps>['items'];
  selectedKeys: string[];
  collapsed?: boolean;
  onNavigate: (path: string) => void;
}

export function AppSidebar({ items, selectedKeys, collapsed = false, onNavigate }: AppSidebarProps) {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  const handleLogout = useCallback(async () => {
    await logout();
    router.replace(ROUTES.login);
  }, [logout, router]);

  const initial = user?.fullName.trim().split(/\s+/).at(-1)?.charAt(0).toUpperCase() ?? '';

  return (
    <div className="flex h-full flex-col">
      <div
        className={clsx(
          'flex h-16 shrink-0 items-center gap-3 border-b border-neutral-100',
          collapsed ? 'justify-center' : 'px-4.5'
        )}
      >
        <BrandMark />
        {!collapsed && <span className="font-semibold text-neutral-900">AcaWrite</span>}
      </div>
      <nav className="flex-1 overflow-y-auto px-3 py-3">
        <Menu
          mode="inline"
          inlineCollapsed={collapsed}
          items={items}
          selectedKeys={selectedKeys}
          onClick={({ key }) => onNavigate(key)}
          className="border-e-0 [&_.ant-menu-item-selected]:font-semibold"
        />
      </nav>
      {user && (
        <div
          className={clsx(
            'border-t border-neutral-100 p-3',
            collapsed ? 'flex flex-col items-center gap-2' : 'flex items-center justify-between gap-2'
          )}
        >
          {collapsed ? (
            <Tooltip title="Đăng xuất" placement="right">
              <button
                type="button"
                onClick={handleLogout}
                aria-label="Đăng xuất"
                className="flex h-9 w-9 cursor-pointer items-center justify-center rounded-lg text-neutral-500 transition-colors hover:bg-neutral-100 hover:text-danger-600 focus:outline-none"
              >
                <LogOut size={16} />
              </button>
            </Tooltip>
          ) : (
            <>
              <div className="flex min-w-0 items-center gap-2">
                <Avatar className="shrink-0 bg-brand-50 font-medium text-brand-700">{initial}</Avatar>
                <div className="min-w-0 leading-tight">
                  <p className="truncate text-caption font-semibold text-neutral-900">{user.fullName}</p>
                  <p className="truncate text-caption text-neutral-500">{ROLE_LABEL[user.role]}</p>
                </div>
              </div>
              <Tooltip title="Đăng xuất">
                <button
                  type="button"
                  onClick={handleLogout}
                  aria-label="Đăng xuất"
                  className="flex h-8 w-8 shrink-0 cursor-pointer items-center justify-center rounded-lg text-neutral-500 transition-colors hover:bg-danger-50 hover:text-danger-600 focus:outline-none"
                >
                  <LogOut size={16} />
                </button>
              </Tooltip>
            </>
          )}
        </div>
      )}
    </div>
  );
}
