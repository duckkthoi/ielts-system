'use client';

import { useCallback, useMemo, useState } from 'react';

import { Drawer } from 'antd';
import clsx from 'clsx';
import { Menu as MenuIcon, PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { usePathname, useRouter } from 'next/navigation';

import { AppHeaderActions } from '@/components/AppHeaderActions';
import { ConnectionBanner } from '@/components/ConnectionBanner';
import { IconButton } from '@/components/IconButton';
import { LAYOUT } from '@/lib/theme/tokens';
import { useAuthGuard } from '@/lib/utils/hooks';

import { AppSidebar } from './AppSidebar';
import { findActiveMenuKey, findPageTitle, MENU_BY_ROLE } from './const';

// ─── Layout có sidebar, dùng cho trang nghiệp vụ của cả 3 vai trò ─────────────
// Từ màn hình lg: sidebar cố định, thu gọn được còn icon. Nhỏ hơn: ẩn sidebar, mở menu bằng drawer.
// Menu đổi theo vai trò; quyền truy cập đường dẫn do useAuthGuard kiểm tra.

export default function SidebarLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { ready, user } = useAuthGuard();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const menuItems = useMemo(() => (user ? MENU_BY_ROLE[user.role] : []), [user]);
  const selectedKeys = useMemo(() => {
    const key = findActiveMenuKey(menuItems, pathname);
    return key ? [key] : [];
  }, [menuItems, pathname]);
  const pageTitle = useMemo(() => findPageTitle(menuItems, pathname), [menuItems, pathname]);

  const handleNavigate = useCallback(
    (path: string) => {
      setMobileMenuOpen(false);
      router.push(path);
    },
    [router]
  );
  const toggleCollapsed = useCallback(() => setCollapsed((value) => !value), []);
  const openMobileMenu = useCallback(() => setMobileMenuOpen(true), []);
  const closeMobileMenu = useCallback(() => setMobileMenuOpen(false), []);

  if (!ready) return null;

  return (
    <div className="flex min-h-screen bg-neutral-25">
      <aside
        className="sticky top-0 hidden h-screen shrink-0 border-r border-neutral-100 bg-neutral-0 transition-[width] duration-200 lg:block"
        style={{ width: collapsed ? LAYOUT.siderCollapsedWidth : LAYOUT.siderWidth }}
      >
        <AppSidebar items={menuItems} selectedKeys={selectedKeys} collapsed={collapsed} onNavigate={handleNavigate} />
      </aside>

      <Drawer
        placement="left"
        size={280}
        open={mobileMenuOpen}
        onClose={closeMobileMenu}
        closable={false}
        styles={{ body: { padding: 0 } }}
      >
        <AppSidebar items={menuItems} selectedKeys={selectedKeys} onNavigate={handleNavigate} />
      </Drawer>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 flex h-16 shrink-0 items-center justify-between gap-3 border-b border-neutral-100 bg-neutral-0 px-4 sm:px-6">
          <div className="flex min-w-0 items-center gap-2">
            <IconButton
              title="Mở menu"
              type="text"
              icon={<MenuIcon size={18} />}
              onClick={openMobileMenu}
              className="lg:hidden"
            />
            <IconButton
              title={collapsed ? 'Mở rộng menu' : 'Thu gọn menu'}
              type="text"
              icon={collapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
              onClick={toggleCollapsed}
              className="hidden lg:inline-flex"
            />
            <div className={clsx('min-w-0 leading-tight', !pageTitle && 'hidden')}>
              <p className="text-caption text-neutral-500">IELTS Writing</p>
              <p className="truncate font-semibold text-neutral-900">{pageTitle}</p>
            </div>
          </div>
          <AppHeaderActions />
        </header>
        <ConnectionBanner />
        <main className="min-w-0 flex-1">{children}</main>
      </div>
    </div>
  );
}
