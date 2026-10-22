import type { ReactNode } from 'react';
import type { DrawerProps } from 'antd';

export interface AppDrawerProps extends Omit<DrawerProps, 'title' | 'width'> {
  /** Tiêu đề của Drawer (string hoặc JSX) */
  title?: ReactNode;
  /** Tiêu đề phụ bên dưới title */
  subtitle?: string;
  /** Nút ở header bên phải */
  extraActions?: ReactNode;
  /** Nút ở footer (Lưu, Huỷ…) */
  footerActions?: ReactNode;
  /** Hiển thị loading bên trong Drawer */
  loading?: boolean;
  /** Bỏ padding mặc định của body */
  noPadding?: boolean;
}
