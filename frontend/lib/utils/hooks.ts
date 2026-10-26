'use client';

import { useCallback, useEffect, useMemo, useState, useSyncExternalStore } from 'react';

import { usePathname, useRouter } from 'next/navigation';

import { ROLE_HOME, ROLE_PATH_PREFIX, ROUTES, SHARED_PATHS } from '@/lib/constants';
import type { ApiResponse } from '@/lib/types/api';
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

/**
 * Tải danh sách phân trang từ server theo `query` (bộ lọc + `page`, `size`); đổi query là tải lại.
 * `loading` suy ra từ việc kết quả đang giữ có ứng với query hiện tại không (không setState đồng bộ trong effect).
 * `reload()` tải lại cùng query (sau khi thêm / sửa / khoá…).
 */
export function useServerList<T, Q extends object>(
  fetcher: (query: Q) => Promise<ApiResponse<T[]>>,
  query: Q
): { data: T[]; total: number; loading: boolean; reload: () => void } {
  const [version, setVersion] = useState(0);
  const [result, setResult] = useState<{ key: string; data: T[]; total: number } | null>(null);
  const queryKey = JSON.stringify(query);
  const key = `${queryKey}#${version}`;

  useEffect(() => {
    let cancelled = false;
    fetcher(JSON.parse(queryKey) as Q)
      .then((res) => {
        if (!cancelled) setResult({ key, data: res.data, total: res.totalRecord ?? res.data.length });
      })
      .catch(() => {
        // axiosClient đã hiển thị toast lỗi; giữ dữ liệu cũ nhưng thoát trạng thái đang tải
        if (!cancelled) setResult((prev) => ({ key, data: prev?.data ?? [], total: prev?.total ?? 0 }));
      });
    return () => {
      cancelled = true;
    };
  }, [fetcher, queryKey, key]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { data: result?.data ?? [], total: result?.total ?? 0, loading: result?.key !== key, reload };
}

// ─── Trạng thái mạng của trình duyệt (sự kiện online / offline) ───────────────

function subscribeOnline(onChange: () => void) {
  window.addEventListener('online', onChange);
  window.addEventListener('offline', onChange);
  return () => {
    window.removeEventListener('online', onChange);
    window.removeEventListener('offline', onChange);
  };
}

/** `false` khi trình duyệt báo mất mạng. Phía server render coi như có mạng. */
export function useOnlineStatus(): boolean {
  return useSyncExternalStore(
    subscribeOnline,
    () => navigator.onLine,
    () => true
  );
}
