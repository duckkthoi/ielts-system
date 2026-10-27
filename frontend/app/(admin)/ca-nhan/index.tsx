'use client';

import { Avatar, Button, Card, Descriptions } from 'antd';
import { KeyRound, LogOut, ShieldCheck } from 'lucide-react';
import { useRouter } from 'next/navigation';

import { PageHeader } from '@/components/PageHeader';
import { ROLE, ROLE_LABEL, ROUTES } from '@/lib/constants';
import { useAuthStore } from '@/stores/useAuthStore';

// ─── Thông tin cá nhân (mọi vai trò) ──────────────────────────────────────────
// Chỉ xem (GET /api/auth/me — store đã nạp khi mở app). Họ tên, email do quản trị viên / giáo viên
// quản lý; người dùng tự đổi được mật khẩu.

export default function ProfilePage() {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  if (!user) return null;

  const handleLogout = async () => {
    await logout();
    router.replace(ROUTES.login);
  };

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-6 sm:px-6">
      <PageHeader title="Thông tin cá nhân" description="Thông tin tài khoản bạn đang dùng." />
      <div className="flex flex-col gap-4">
        <Card>
          <div className="mb-5 flex items-center gap-4">
            <Avatar size={56} className="bg-brand-50! text-h2! text-brand-700!">
              {user.fullName.split(' ').pop()?.charAt(0)}
            </Avatar>
            <div className="min-w-0">
              <p className="truncate text-h2 font-semibold text-neutral-900">{user.fullName}</p>
              <p className="text-neutral-500">{ROLE_LABEL[user.role]}</p>
            </div>
          </div>
          <Descriptions
            column={1}
            size="small"
            items={[
              { key: 'username', label: 'Tên đăng nhập', children: <span className="font-mono">{user.username}</span> },
              ...(user.role === ROLE.STUDENT
                ? [
                    {
                      key: 'code',
                      label: 'Mã sinh viên',
                      children: <span className="font-mono">{user.studentCode ?? '—'}</span>
                    }
                  ]
                : []),
              {
                key: 'email',
                label: 'Email',
                children: user.email ?? <span className="text-neutral-400">Chưa có</span>
              },
              { key: 'role', label: 'Vai trò', children: ROLE_LABEL[user.role] }
            ]}
          />
          <p className="mt-4 text-small text-neutral-500">
            {user.role === ROLE.STUDENT
              ? 'Cần sửa họ tên hoặc email? Hãy báo giáo viên phụ trách lớp.'
              : 'Cần sửa họ tên hoặc email? Hãy liên hệ quản trị viên hệ thống.'}
          </p>
        </Card>

        <Card
          title={
            <span className="inline-flex items-center gap-2">
              <ShieldCheck size={18} className="text-brand-600" /> Bảo mật
            </span>
          }
        >
          <ul className="mb-4 list-disc pl-5 text-neutral-700">
            <li>Mỗi tài khoản chỉ đăng nhập ở một nơi: đăng nhập ở máy khác thì máy này tự đăng xuất.</li>
            <li>Đổi mật khẩu sẽ đăng xuất các thiết bị khác, giữ lại phiên đang dùng.</li>
          </ul>
          <div className="flex flex-wrap gap-2">
            <Button type="primary" icon={<KeyRound size={16} />} onClick={() => router.push(ROUTES.changePassword)}>
              Đổi mật khẩu
            </Button>
            <Button danger icon={<LogOut size={16} />} onClick={handleLogout}>
              Đăng xuất
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
