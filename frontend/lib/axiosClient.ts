import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import debounce from 'lodash/debounce';

import { ApiError } from '@/lib/apiError';
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

// ─── Refresh token (một lần cho nhiều request bị 401 cùng lúc) ─────────────────

let refreshPromise: Promise<string | null> | null = null;

/** Làm mới access token (dùng chung với WebSocket khi kết nối lại). Thất bại → null. */
export async function refreshAccessToken(): Promise<string | null> {
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

// ─── Xử lý lỗi ────────────────────────────────────────────────────────────────
// Server luôn trả HTTP 200; lỗi là body { success: false, errorCode, errorMessage, status } (docs/05 mục A.1).
// `status` trong body là mã thật (401, 404, 409…) — 401 thì refresh token một lần rồi gửi lại request.

type RetryConfig = InternalAxiosRequestConfig & { _retry?: boolean };

/** File tải về (blob) mà server trả JSON → đó là body lỗi, đọc ra để xử lý như mọi lỗi khác. */
async function readBlobError(response: AxiosResponse): Promise<ApiErrorBody | null> {
  const data: unknown = response.data;
  if (!(data instanceof Blob) || !data.type.includes('application/json')) return null;
  const body = JSON.parse(await data.text()) as ApiErrorBody | ApiResponse<unknown>;
  return body.success ? null : (body as ApiErrorBody);
}

async function handleFailure(config: RetryConfig | undefined, body: ApiErrorBody | undefined, httpStatus?: number) {
  const status = body?.status ?? httpStatus;

  if (status === 401 && config && !config._retry && !config.isAuthCall) {
    config._retry = true;
    const token = await refreshAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      return axiosClient(config);
    }
    redirectToLogin();
  }

  const msg = body?.errorMessage || (httpStatus ? 'Thao tác thất bại' : 'Không kết nối được tới máy chủ');
  if (config?.toastError !== false) toastError(msg);
  return Promise.reject(new ApiError(msg, { code: body?.errorCode, status, fieldErrors: body?.fieldErrors }));
}

axiosClient.interceptors.response.use(
  async (response: AxiosResponse) => {
    if (!response.config.controlLoading) hideLoading();
    const config = response.config as RetryConfig;

    if (config.rawResponse) {
      const blobError = await readBlobError(response);
      return blobError ? handleFailure(config, blobError, response.status) : response;
    }

    const body = response.data as ApiResponse<unknown> | ApiErrorBody;
    if (!body.success) return handleFailure(config, body as ApiErrorBody, response.status);
    // Caller nhận thẳng ApiResponse<T> — cast có chủ đích.
    return body as unknown as AxiosResponse;
  },
  async (error: AxiosError<ApiErrorBody>) => {
    // Chỉ còn lỗi mạng / timeout / lỗi ngoài ứng dụng (vd nginx 502) — server ứng dụng luôn trả 200.
    const config = error.config as RetryConfig | undefined;
    if (!config?.controlLoading) hideLoading();
    return handleFailure(config, error.response?.data, error.response?.status);
  }
);

export default axiosClient;
