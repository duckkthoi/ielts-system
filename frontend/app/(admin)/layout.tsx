'use client';

import { useCallback, useMemo } from 'react';

import { Layout, Menu } from 'antd';
import type { MenuProps } from 'antd';
import { usePathname, useRouter } from 'next/navigation';

import { AppHeaderActions } from '@/components/AppHeaderActions';
import { LAYOUT } from '@/lib/theme/tokens';
import { useAuthGuard } from '@/lib/utils/hooks';

import { findActiveMenuKey, MENU_BY_ROLE } from './const';

// ─── Layout có sidebar, dùng cho trang nghiệp vụ của cả 3 vai trò ─────────────
// Menu đổi theo vai trò; quyền truy cập đường dẫn do useAuthGuard kiểm tra.
// Màu sắc và kích thước lấy từ design system (lib/theme). Giao diện chi tiết làm theo bản Figma.

export default function SidebarLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { ready, user } = useAuthGuard();

  const menuItems = useMemo(() => (user ? MENU_BY_ROLE[user.role] : []), [user]);
  const selectedKeys = useMemo(() => {
    const key = findActiveMenuKey(menuItems, pathname);
    return key ? [key] : [];
  }, [menuItems, pathname]);

  const handleMenuClick = useCallback<NonNullable<MenuProps['onClick']>>(({ key }) => router.push(key), [router]);

  if (!ready) return null;

  return (
    <Layout className="min-h-screen">
      <Layout.Sider width={LAYOUT.siderWidth} breakpoint="lg" collapsedWidth={LAYOUT.siderCollapsedWidth} theme="light">
        <div className="flex h-16 items-center px-4 font-semibold">IELTS Writing</div>
        <Menu mode="inline" items={menuItems} selectedKeys={selectedKeys} onClick={handleMenuClick} />
      </Layout.Sider>
      <Layout>
        <Layout.Header className="flex items-center justify-end px-4">
          <AppHeaderActions />
        </Layout.Header>
        <Layout.Content>{children}</Layout.Content>
      </Layout>
    </Layout>
  );
}
