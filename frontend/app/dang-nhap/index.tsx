'use client';

import { useCallback, useEffect, useState } from 'react';

import { Alert, Button, Card, Form, Input } from 'antd';
import { Bell, ClipboardCheck, Eye, EyeOff, LockKeyhole, Save, UserRound } from 'lucide-react';
import { useRouter, useSearchParams } from 'next/navigation';

import { BrandMark } from '@/components/BrandMark';
import { ApiError, getErrorMessage } from '@/lib/apiError';
import { ROUTES } from '@/lib/constants';
import { canAccessPath } from '@/lib/utils/hooks';
import type { AccountInfo } from '@/lib/types/auth';
import { getHomePath, useAuthStore } from '@/stores/useAuthStore';

import type { LoginError, LoginFormValues } from './type';

// ─── Helper ───────────────────────────────────────────────────────────────────

/** Chỉ chấp nhận returnUrl nội bộ mà vai trò được phép vào, tránh open redirect. */
function resolveTarget(user: AccountInfo, returnUrl: string | null): string {
  if (user.mustChangePassword) return ROUTES.changePassword;
  if (returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//') && canAccessPath(user, returnUrl)) {
    return returnUrl;
  }
  return getHomePath(user);
}

/** 429 (đăng nhập sai quá nhiều lần) hiện cảnh báo vàng; lỗi khác hiện đỏ. Nội dung lấy từ server. */
function toLoginError(error: unknown): LoginError {
  const limited = error instanceof ApiError && error.code === 'RATE_LIMITED';
  return { type: limited ? 'warning' : 'error', message: getErrorMessage(error, 'Đăng nhập thất bại') };
}

const BENEFITS = [
  { icon: <Save size={17} />, label: 'Tự lưu bài khi đang viết' },
  { icon: <ClipboardCheck size={17} />, label: 'Chấm theo tiêu chí và checklist' },
  { icon: <Bell size={17} />, label: 'Thông báo tức thì khi có kết quả' }
];

// ─── Panel giới thiệu (chỉ hiện từ màn hình lg) ───────────────────────────────

function BrandPanel() {
  return (
    <section className="relative hidden flex-col justify-center overflow-hidden bg-brand-600 p-16 text-neutral-0 lg:flex">
      <span className="pointer-events-none absolute -top-52 -right-30 size-105 rounded-full border border-neutral-0/10" />
      <span className="pointer-events-none absolute -bottom-40 -left-45 size-80 rounded-full border border-neutral-0/10" />
      <div className="relative max-w-125">
        <BrandMark size="lg" onBrand />
        <p className="mt-4 text-h3 font-semibold">IELTS Writing</p>
        <h1 className="mt-5 mb-3 text-[34px] leading-tight font-semibold">
          Giao bài và chấm bài IELTS Writing trực tuyến
        </h1>
        <p className="max-w-115 text-prompt text-neutral-0/70">
          Không gian học thuật tập trung cho toàn bộ quá trình luyện viết.
        </p>
        <ul className="mt-7 flex flex-col gap-4">
          {BENEFITS.map((item) => (
            <li key={item.label} className="flex items-center gap-3">
              <span className="grid size-8.5 place-items-center rounded-[9px] bg-neutral-0/15">{item.icon}</span>
              {item.label}
            </li>
          ))}
        </ul>
      </div>
      <p className="absolute bottom-8 left-16 text-caption text-neutral-0/55">© 2026 IELTS Writing</p>
    </section>
  );
}

// ─── Component ────────────────────────────────────────────────────────────────

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const login = useAuthStore((s) => s.login);
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isInitialized = useAuthStore((s) => s.isInitialized);
  const [submitting, setSubmitting] = useState(false);
  const [loginError, setLoginError] = useState<LoginError | null>(null);

  const returnUrl = searchParams.get('returnUrl');

  const handleSubmit = useCallback(
    async (values: LoginFormValues) => {
      setSubmitting(true);
      setLoginError(null);
      try {
        const loggedIn = await login(values);
        router.replace(resolveTarget(loggedIn, returnUrl));
      } catch (error) {
        setLoginError(toLoginError(error));
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
    <main className="grid min-h-screen lg:grid-cols-2">
      <BrandPanel />
      <section className="grid place-items-center bg-neutral-25 px-4 py-12 sm:px-12">
        <Card className="w-full max-w-100 border-0 shadow-md" styles={{ body: { padding: 32 } }}>
          <div className="mb-7 flex items-center gap-2.5 lg:hidden">
            <BrandMark />
            <span className="font-semibold text-neutral-900">IELTS Writing</span>
          </div>
          <h1 className="mb-2 text-h1 font-semibold text-neutral-900">Đăng nhập</h1>
          <p className="text-neutral-500">Chào mừng bạn trở lại. Vui lòng nhập thông tin tài khoản.</p>
          {loginError && <Alert className="mt-5" type={loginError.type} showIcon title={loginError.message} />}
          <Form<LoginFormValues>
            className="mt-6"
            layout="vertical"
            onFinish={handleSubmit}
            requiredMark={false}
            size="large"
          >
            <Form.Item
              name="username"
              label="Tên đăng nhập / Mã sinh viên"
              rules={[{ required: true, message: 'Vui lòng nhập tên đăng nhập' }]}
            >
              <Input
                prefix={<UserRound size={16} className="text-neutral-400" />}
                placeholder="Nhập tên đăng nhập"
                autoComplete="username"
                autoFocus
              />
            </Form.Item>
            <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}>
              <Input.Password
                prefix={<LockKeyhole size={16} className="text-neutral-400" />}
                placeholder="Nhập mật khẩu"
                autoComplete="current-password"
                iconRender={(visible) => (visible ? <Eye size={16} /> : <EyeOff size={16} />)}
              />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={submitting}>
              Đăng nhập
            </Button>
          </Form>
          <p className="mt-5 text-center text-caption text-neutral-500">
            Quên mật khẩu? Liên hệ giáo viên hoặc quản trị viên.
          </p>
        </Card>
      </section>
    </main>
  );
}
