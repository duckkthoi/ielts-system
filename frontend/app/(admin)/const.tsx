import type { MenuProps } from 'antd';
import {
  BarChart3,
  BookOpen,
  ClipboardCheck,
  FileText,
  History,
  LayoutDashboard,
  Library,
  MessageSquare,
  ScrollText,
  Trophy,
  UserCog,
  Users
} from 'lucide-react';

import { ROLE } from '@/lib/constants';
import type { Role } from '@/lib/constants';

// ─── Menu sidebar theo vai trò ────────────────────────────────────────────────
// key = đường dẫn, để Menu tự highlight theo pathname.

type MenuItem = Required<MenuProps>['items'][number];

const ICON_SIZE = 16;

export const MENU_BY_ROLE: Record<Role, MenuItem[]> = {
  [ROLE.ADMIN]: [
    { key: '/quan-tri/tong-quan', icon: <LayoutDashboard size={ICON_SIZE} />, label: 'Tổng quan' },
    { key: '/quan-tri/giao-vien', icon: <UserCog size={ICON_SIZE} />, label: 'Giáo viên' },
    { key: '/quan-tri/nguoi-dung', icon: <Users size={ICON_SIZE} />, label: 'Người dùng' },
    { key: '/quan-tri/nhat-ky', icon: <ScrollText size={ICON_SIZE} />, label: 'Nhật ký hệ thống' },
    { key: '/quan-tri/phien-dang-nhap', icon: <History size={ICON_SIZE} />, label: 'Phiên đăng nhập' }
  ],
  [ROLE.TEACHER]: [
    { key: '/giao-vien/tong-quan', icon: <LayoutDashboard size={ICON_SIZE} />, label: 'Tổng quan' },
    { key: '/giao-vien/lop-hoc', icon: <Users size={ICON_SIZE} />, label: 'Lớp học' },
    { key: '/giao-vien/bai-tap', icon: <FileText size={ICON_SIZE} />, label: 'Bài tập' },
    { key: '/giao-vien/cham-bai', icon: <ClipboardCheck size={ICON_SIZE} />, label: 'Chấm bài' },
    { key: '/giao-vien/ngan-hang-de', icon: <Library size={ICON_SIZE} />, label: 'Ngân hàng đề' },
    { key: '/giao-vien/thong-ke', icon: <BarChart3 size={ICON_SIZE} />, label: 'Thống kê' },
    { key: '/chat', icon: <MessageSquare size={ICON_SIZE} />, label: 'Trao đổi' }
  ],
  [ROLE.STUDENT]: [
    { key: '/sinh-vien/tong-quan', icon: <BookOpen size={ICON_SIZE} />, label: 'Bài được giao' },
    { key: '/sinh-vien/ket-qua', icon: <Trophy size={ICON_SIZE} />, label: 'Kết quả học tập' },
    { key: '/chat', icon: <MessageSquare size={ICON_SIZE} />, label: 'Trao đổi' }
  ]
};

/** Tiêu đề trên header cho trang dùng chung không nằm trong menu. */
const SHARED_PAGE_TITLE: Record<string, string> = {
  '/thong-bao': 'Thông báo',
  '/ca-nhan': 'Thông tin cá nhân'
};

/** Trang con không nằm dưới đường dẫn mục menu của nó: chi tiết bài của sinh viên thuộc "Bài được giao". */
const MENU_ALIAS: Record<string, string> = {
  '/sinh-vien/bai-tap': '/sinh-vien/tong-quan'
};

/** Tìm key menu đang active: mục có đường dẫn là tiền tố dài nhất của pathname (sau khi đổi theo MENU_ALIAS). */
export function findActiveMenuKey(items: MenuItem[], rawPathname: string): string | undefined {
  const alias = Object.keys(MENU_ALIAS).find((p) => rawPathname === p || rawPathname.startsWith(`${p}/`));
  const pathname = alias ? MENU_ALIAS[alias] : rawPathname;
  return items
    .map((item) => (item && 'key' in item ? String(item.key) : ''))
    .filter((key) => key && (pathname === key || pathname.startsWith(`${key}/`)))
    .sort((a, b) => b.length - a.length)[0];
}

/** Tiêu đề trang hiện tại trên header: nhãn mục menu đang active, hoặc tên trang dùng chung. */
export function findPageTitle(items: MenuItem[], pathname: string): string {
  const key = findActiveMenuKey(items, pathname);
  const item = items.find((i) => i && 'key' in i && i.key === key);
  if (item && 'label' in item && typeof item.label === 'string') return item.label;
  const shared = Object.keys(SHARED_PAGE_TITLE).find((p) => pathname === p || pathname.startsWith(`${p}/`));
  return shared ? SHARED_PAGE_TITLE[shared] : '';
}
