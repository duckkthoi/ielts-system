'use client';

import { useCallback, useEffect, useState } from 'react';

import { Button, Form, Input } from 'antd';
import { useRouter, useSearchParams } from 'next/navigation';

import { ROUTES } from '@/lib/constants';
import { canAccessPath } from '@/lib/utils/hooks';
import type { AccountInfo } from '@/lib/types/auth';
import { getHomePath, useAuthStore } from '@/stores/useAuthStore';

import type { LoginFormValues } from './type';

// ─── Helper ───────────────────────────────────────────────────────────────────

/** Chỉ chấp nhận returnUrl nội bộ mà vai trò được phép vào, tránh open redirect. */
function resolveTarget(user: AccountInfo, returnUrl: string | null): string {
  if (user.mustChangePassword) return ROUTES.changePassword;
  if (returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//') && canAccessPath(user, returnUrl)) {
    return returnUrl;
  }
  return getHomePath(user);
}

// ─── Component ────────────────────────────────────────────────────────────────
// Bố cục và hình ảnh trang đăng nhập sẽ làm theo bản thiết kế Figma.

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const login = useAuthStore((s) => s.login);
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isInitialized = useAuthStore((s) => s.isInitialized);
  const [submitting, setSubmitting] = useState(false);

  const returnUrl = searchParams.get('returnUrl');

  const handleSubmit = useCallback(
    async (values: LoginFormValues) => {
      setSubmitting(true);
      try {
        const loggedIn = await login(values);
        router.replace(resolveTarget(loggedIn, returnUrl));
      } catch {
        // axiosClient đã hiển thị toast lỗi
      } finally {
        setSubmitting(false);
      }
    },
    [login, router, returnUrl]
  );

  useEffect(() => {
    if (isInitialized && isAuthenticated && user) {
      router.replace(resolveTarget(user, returnUrl));
    }
  }, [isInitialized, isAuthenticated, user, router, returnUrl]);

  return (
    <main className="flex min-h-screen items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-6 text-2xl font-semibold">Đăng nhập</h1>
        <Form<LoginFormValues> layout="vertical" onFinish={handleSubmit} requiredMark={false}>
          <Form.Item
            name="username"
            label="Tên đăng nhập / Mã sinh viên"
            rules={[{ required: true, message: 'Vui lòng nhập tên đăng nhập' }]}
          >
            <Input autoComplete="username" autoFocus />
          </Form.Item>
          <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            Đăng nhập
          </Button>
        </Form>
      </div>
    </main>
  );
}
