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

/** Tìm key menu đang active: mục có đường dẫn là tiền tố dài nhất của pathname. */
export function findActiveMenuKey(items: MenuItem[], pathname: string): string | undefined {
  return items
    .map((item) => (item && 'key' in item ? String(item.key) : ''))
    .filter((key) => key && (pathname === key || pathname.startsWith(`${key}/`)))
    .sort((a, b) => b.length - a.length)[0];
}
