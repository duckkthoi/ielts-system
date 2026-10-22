import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import debounce from 'lodash/debounce';

import { ROUTES } from '@/lib/constants';
import { clearStoredTokens, getAccessToken, getRefreshToken, saveTokens } from '@/lib/tokens';
import type { ApiErrorBody, ApiResponse } from '@/lib/types/api';
import type { TokenPair } from '@/lib/types/auth';
import { getMessage, useAppStore } from '@/stores/useAppStore';

// ─── Module augmentation ─────────────────────────────────────────────────────
// controlLoading: true  → caller tự quản lý loading (không bật spinner toàn cục)
// toastError: false     → tắt toast lỗi tự động (mặc định true)
// isAuthCall: true      → request đăng nhập / refresh — không gắn token, không tự refresh
// rawResponse: true     → giữ nguyên body (dùng cho tải file blob)
declare module 'axios' {
  interface AxiosRequestConfig {
    controlLoading?: boolean;
    toastError?: boolean;
    isAuthCall?: boolean;
    rawResponse?: boolean;
  }
}

// ─── Global loading ───────────────────────────────────────────────────────────
// Đếm số request đang chạy; debounce 100ms khi tắt để không nháy giữa các request liên tiếp.

let activeRequests = 0;

const debouncedHide = debounce(() => {
  if (activeRequests === 0) useAppStore.getState().setLoading(false);
}, 100);

function showLoading() {
  if (typeof window === 'undefined') return;
  debouncedHide.cancel();
  activeRequests++;
  if (activeRequests === 1) useAppStore.getState().setLoading(true);
}

function hideLoading() {
  if (typeof window === 'undefined') return;
  activeRequests = Math.max(0, activeRequests - 1);
  if (activeRequests === 0) debouncedHide();
}

function toastError(message: string) {
  if (typeof window === 'undefined') return;
  getMessage()?.error(message);
}

function redirectToLogin() {
  if (typeof window === 'undefined') return;
  clearStoredTokens();
  const returnUrl = encodeURIComponent(window.location.pathname + window.location.search);
  // Interceptor chạy ngoài React nên không dùng được router; tải lại trang để xoá sạch state phiên cũ.
  // eslint-disable-next-line @next/next/no-location-assign-relative-destination
  window.location.href = `${ROUTES.login}?returnUrl=${returnUrl}`;
}

// ─── Axios instance ───────────────────────────────────────────────────────────

const axiosClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
  timeout: 60_000,
  headers: { 'Content-Type': 'application/json' }
});

// ─── Refresh token (một lần cho nhiều request 401 cùng lúc) ───────────────────

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;
  refreshPromise ??= axiosClient
    .post('/api/auth/refresh', { refreshToken }, { isAuthCall: true, toastError: false, controlLoading: true })
    .then((res) => {
      const tokens = (res as unknown as ApiResponse<TokenPair>).data;
      saveTokens(tokens);
      return tokens.accessToken;
    })
    .catch(() => {
      // 401 REFRESH_TOKEN_ROTATED: tab khác vừa refresh xong và đã lưu token mới vào localStorage → dùng luôn.
      const stored = getRefreshToken();
      return stored && stored !== refreshToken ? getAccessToken() : null;
    })
    .finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
}

axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (!config.isAuthCall) {
      const token = getAccessToken();
      if (token) config.headers.Authorization = `Bearer ${token}`;
    }
    if (!config.controlLoading) showLoading();
    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response: AxiosResponse) => {
    if (!response.config.controlLoading) hideLoading();
    if (response.config.rawResponse) return response;

    const body = response.data as ApiResponse<unknown>;
    if (!body.success) {
      const msg = body.message || 'Thao tác thất bại';
      if (response.config.toastError !== false) toastError(msg);
      return Promise.reject(new Error(msg));
    }
    // Caller nhận thẳng ApiResponse<T> — cast có chủ đích.
    return body as unknown as AxiosResponse;
  },
  async (error: AxiosError<ApiErrorBody>) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    if (!original?.controlLoading) hideLoading();

    if (error.response?.status === 401 && original && !original._retry && !original.isAuthCall) {
      original._retry = true;
      const token = await refreshAccessToken();
      if (token) {
        original.headers.Authorization = `Bearer ${token}`;
        return axiosClient(original);
      }
      redirectToLogin();
      return Promise.reject(error);
    }

    const body = error.response?.data;
    const msg = body?.message || (error.response ? 'Thao tác thất bại' : 'Không kết nối được tới máy chủ');
    if (original?.toastError !== false) toastError(msg);
    return Promise.reject(new Error(msg));
  }
);

export default axiosClient;
