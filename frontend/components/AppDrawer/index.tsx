'use client';

import { Drawer, Spin } from 'antd';
import clsx from 'clsx';

import type { AppDrawerProps } from './type';

// Giao diện chi tiết (màu, viền, khoảng cách) sẽ chỉnh theo design system sau khi có Figma.

export function AppDrawer({
  title,
  subtitle,
  extraActions,
  footerActions,
  loading = false,
  noPadding = false,
  children,
  size = 600,
  placement = 'right',
  className,
  ...restProps
}: AppDrawerProps) {
  const hasHeader = Boolean(title || subtitle || extraActions);

  const headerTitle = hasHeader ? (
    <div className="min-w-0 flex-1">
      {typeof title === 'string' ? <h3 className="truncate text-lg font-semibold">{title}</h3> : title}
      {subtitle && <p className="mt-0.5 truncate text-xs font-normal opacity-70">{subtitle}</p>}
    </div>
  ) : null;

  return (
    <Drawer
      size={size}
      placement={placement}
      title={headerTitle}
      extra={extraActions}
      className={clsx('app-drawer-root', className)}
      footer={footerActions ? <div className="flex items-center justify-end gap-3">{footerActions}</div> : undefined}
      {...restProps}
    >
      <Spin spinning={loading}>
        <div className={clsx(noPadding && '-m-6')}>{children}</div>
      </Spin>
    </Drawer>
  );
}

export default AppDrawer;
