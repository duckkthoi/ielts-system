'use client';

import { Menu } from 'antd';
import type { MenuProps } from 'antd';
import clsx from 'clsx';

import { BrandMark } from '@/components/BrandMark';

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
    </div>
  );
}
