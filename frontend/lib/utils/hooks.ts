'use client';

import { useEffect, useMemo } from 'react';

import { usePathname, useRouter } from 'next/navigation';

import { ROLE_HOME, ROLE_PATH_PREFIX, ROUTES, SHARED_PATHS } from '@/lib/constants';
import type { AccountInfo } from '@/lib/types/auth';
import { useAuthStore } from '@/stores/useAuthStore';

// ─── Hook dùng toàn app ───────────────────────────────────────────────────────

/** Vai trò có được vào đường dẫn này không. */
export function canAccessPath(user: AccountInfo, pathname: string): boolean {
  if (SHARED_PATHS.some((p) => pathname === p || pathname.startsWith(`${p}/`))) return true;
  return pathname.startsWith(ROLE_PATH_PREFIX[user.role]);
}

/**
 * Bảo vệ các layout cần đăng nhập:
 * - chưa đăng nhập → /dang-nhap?returnUrl=…
 * - phải đổi mật khẩu lần đầu → /doi-mat-khau
 * - sai vai trò → trang chủ của vai trò
 * Trả về `ready = true` khi được phép render nội dung.
 */
export function useAuthGuard(): { ready: boolean; user: AccountInfo | null } {
  const router = useRouter();
  const pathname = usePathname();
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isInitialized = useAuthStore((s) => s.isInitialized);

  const redirectTo = useMemo(() => {
    if (!isInitialized) return null;
    if (!isAuthenticated || !user) return `${ROUTES.login}?returnUrl=${encodeURIComponent(pathname)}`;
    if (user.mustChangePassword) return ROUTES.changePassword;
    if (!canAccessPath(user, pathname)) return ROLE_HOME[user.role];
    return null;
  }, [isInitialized, isAuthenticated, user, pathname]);

  useEffect(() => {
    if (redirectTo) router.replace(redirectTo);
  }, [redirectTo, router]);

  return { ready: isInitialized && !redirectTo, user };
}
