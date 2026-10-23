'use client';

import { useCallback, useEffect, useState } from 'react';

import { Alert, App, Button, Card, Form, Input } from 'antd';
import clsx from 'clsx';
import { ArrowLeft, Check, KeyRound, LockKeyhole, Save } from 'lucide-react';
import { useRouter } from 'next/navigation';

import { authApi } from '@/lib/api/auth';
import { ROUTES } from '@/lib/constants';
import { getHomePath, useAuthStore } from '@/stores/useAuthStore';

import { CONFIRM_PASSWORD_RULES, NEW_PASSWORD_RULES, PASSWORD_CHECKS } from './const';
import type { ChangePasswordFormValues } from './type';

// ─── Danh sách điều kiện mật khẩu ─────────────────────────────────────────────

function PasswordChecks({ password, current }: { password: string; current: string }) {
  return (
    <ul className="-mt-2 mb-5 flex flex-col gap-2">
      {PASSWORD_CHECKS.map((check) => {
        const passed = check.passed(password, current);
        return (
          <li key={check.label} className={clsx('flex items-center gap-2', passed && 'text-success')}>
            <span
              className={clsx(
                'grid size-4.5 place-items-center rounded-full border',
                passed ? 'border-success bg-success text-neutral-0' : 'border-neutral-300'
              )}
            >
              {passed && <Check size={12} strokeWidth={3} />}
            </span>
            {check.label}
          </li>
        );
      })}
    </ul>
  );
}

// ─── Component ────────────────────────────────────────────────────────────────
// Dùng cho cả lần đăng nhập đầu (bắt buộc) và đổi mật khẩu chủ động.

export default function ChangePasswordPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [form] = Form.useForm<ChangePasswordFormValues>();
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isInitialized = useAuthStore((s) => s.isInitialized);
  const setUser = useAuthStore((s) => s.setUser);
  const [submitting, setSubmitting] = useState(false);

  const newPassword = Form.useWatch('newPassword', form) ?? '';
  const currentPassword = Form.useWatch('currentPassword', form) ?? '';

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

  const handleBack = useCallback(() => router.back(), [router]);

  useEffect(() => {
    if (isInitialized && !isAuthenticated) router.replace(ROUTES.login);
  }, [isInitialized, isAuthenticated, router]);

  if (!user) return null;

  return (
    <main className="grid min-h-screen place-items-center bg-neutral-25 px-4 py-12">
      <Card className="w-full max-w-110 border-0 shadow-md" styles={{ body: { padding: 32 } }}>
        {!user.mustChangePassword && (
          <Button type="text" className="-ml-3 mb-4" icon={<ArrowLeft size={16} />} onClick={handleBack}>
            Quay lại
          </Button>
        )}
        <span className="mb-5 grid size-13 place-items-center rounded-[14px] bg-brand-50 text-brand-600">
          <KeyRound size={24} />
        </span>
        <h1 className="mb-2 text-h1 font-semibold text-neutral-900">Đổi mật khẩu</h1>
        <p className="text-neutral-500">Thiết lập mật khẩu riêng để bảo vệ tài khoản của bạn.</p>
        {user.mustChangePassword && (
          <Alert
            className="mt-5"
            type="info"
            showIcon
            title="Đây là lần đăng nhập đầu tiên, vui lòng đặt mật khẩu mới."
          />
        )}
        <Form<ChangePasswordFormValues>
          form={form}
          className="mt-6"
          layout="vertical"
          onFinish={handleSubmit}
          requiredMark={false}
          size="large"
        >
          <Form.Item
            name="currentPassword"
            label="Mật khẩu hiện tại"
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại' }]}
          >
            <Input.Password
              prefix={<LockKeyhole size={16} className="text-neutral-400" />}
              placeholder={user.mustChangePassword ? 'Nhập mật khẩu tạm' : 'Nhập mật khẩu hiện tại'}
              autoComplete="current-password"
            />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="Mật khẩu mới"
            rules={NEW_PASSWORD_RULES}
            dependencies={['currentPassword']}
          >
            <Input.Password prefix={<KeyRound size={16} className="text-neutral-400" />} autoComplete="new-password" />
          </Form.Item>
          <PasswordChecks password={newPassword} current={currentPassword} />
          <Form.Item
            name="confirmPassword"
            label="Nhập lại mật khẩu mới"
            dependencies={['newPassword']}
            rules={CONFIRM_PASSWORD_RULES}
          >
            <Input.Password prefix={<KeyRound size={16} className="text-neutral-400" />} autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting} icon={<Save size={16} />}>
            Lưu mật khẩu
          </Button>
        </Form>
      </Card>
    </main>
  );
}
