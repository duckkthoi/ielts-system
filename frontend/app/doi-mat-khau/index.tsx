'use client';

import { useCallback, useEffect, useState } from 'react';

import { App, Button, Form, Input } from 'antd';
import { useRouter } from 'next/navigation';

import { authApi } from '@/lib/api/auth';
import { ROUTES } from '@/lib/constants';
import { getHomePath, useAuthStore } from '@/stores/useAuthStore';

import { CONFIRM_PASSWORD_RULES, NEW_PASSWORD_RULES } from './const';
import type { ChangePasswordFormValues } from './type';

// ─── Component ────────────────────────────────────────────────────────────────
// Dùng cho cả lần đăng nhập đầu (bắt buộc) và đổi mật khẩu chủ động.

export default function ChangePasswordPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isInitialized = useAuthStore((s) => s.isInitialized);
  const setUser = useAuthStore((s) => s.setUser);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = useCallback(
    async ({ currentPassword, newPassword }: ChangePasswordFormValues) => {
      if (!user) return;
      setSubmitting(true);
      try {
        await authApi.changePassword({ currentPassword, newPassword });
        const updated = { ...user, mustChangePassword: false };
        setUser(updated);
        message.success('Đổi mật khẩu thành công');
        router.replace(getHomePath(updated));
      } catch {
        // axiosClient đã hiển thị toast lỗi
      } finally {
        setSubmitting(false);
      }
    },
    [user, setUser, message, router]
  );

  useEffect(() => {
    if (isInitialized && !isAuthenticated) router.replace(ROUTES.login);
  }, [isInitialized, isAuthenticated, router]);

  if (!user) return null;

  return (
    <main className="flex min-h-screen items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-2 text-2xl font-semibold">Đổi mật khẩu</h1>
        {user.mustChangePassword && (
          <p className="mb-6 text-sm opacity-70">Đây là lần đăng nhập đầu tiên, vui lòng đặt mật khẩu mới.</p>
        )}
        <Form<ChangePasswordFormValues> layout="vertical" onFinish={handleSubmit} requiredMark={false}>
          <Form.Item
            name="currentPassword"
            label="Mật khẩu hiện tại"
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại' }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item name="newPassword" label="Mật khẩu mới" rules={NEW_PASSWORD_RULES}>
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="Nhập lại mật khẩu mới"
            dependencies={['newPassword']}
            rules={CONFIRM_PASSWORD_RULES}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            Lưu mật khẩu
          </Button>
        </Form>
      </div>
    </main>
  );
}
