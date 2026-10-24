import axiosClient from '@/lib/axiosClient';
import type { ApiResponse } from '@/lib/types/api';
import type { AccountInfo, ChangePasswordRequest, LoginRequest, LoginResponse } from '@/lib/types/auth';

// ─── Xác thực ─────────────────────────────────────────────────────────────────

/** Trang đăng nhập tự hiện lỗi ngay trên form (sai thông tin, bị khoá, đăng nhập sai quá nhiều lần) nên tắt toast. */
export async function login(data: LoginRequest): Promise<LoginResponse> {
  const res = (await axiosClient.post('/api/auth/login', data, {
    isAuthCall: true,
    toastError: false
  })) as unknown as ApiResponse<LoginResponse>;
  return res.data;
}

export async function getMe(): Promise<AccountInfo> {
  const res = (await axiosClient.get('/api/auth/me', { controlLoading: true })) as unknown as ApiResponse<AccountInfo>;
  return res.data;
}

export async function changePassword(data: ChangePasswordRequest): Promise<void> {
  await axiosClient.post('/api/auth/change-password', data);
}

export async function logout(refreshToken: string): Promise<void> {
  await axiosClient.post('/api/auth/logout', { refreshToken }, { toastError: false, controlLoading: true });
}

export const authApi = { login, getMe, changePassword, logout };
