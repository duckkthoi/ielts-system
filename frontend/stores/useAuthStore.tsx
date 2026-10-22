'use client';

import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

import { authApi } from '@/lib/api/auth';
import { ROLE, ROLE_HOME } from '@/lib/constants';
import { appStorage, STORAGE_KEYS } from '@/lib/storage';
import { clearStoredTokens, getAccessToken, getRefreshToken, saveTokens } from '@/lib/tokens';
import type { AccountInfo, LoginRequest } from '@/lib/types/auth';

// ─── Store ────────────────────────────────────────────────────────────────────

export interface AuthState {
  user: AccountInfo | null;
  isAuthenticated: boolean;
  /** Đã kiểm tra xong phiên đăng nhập khi mở app — không persist. */
  isInitialized: boolean;
  /** Đăng nhập, lưu token + user. Trả về user để caller điều hướng theo vai trò. */
  login: (data: LoginRequest) => Promise<AccountInfo>;
  /** Gọi /api/auth/me để làm mới thông tin user (khi mở app có sẵn token). */
  loadSession: () => Promise<void>;
  /** Xoá user/token, KHÔNG điều hướng. */
  clearSession: () => void;
  /** Báo server thu hồi refresh token rồi xoá phiên. */
  logout: () => Promise<void>;
  setUser: (user: AccountInfo) => void;
  setInitialized: (value: boolean) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      isAuthenticated: false,
      isInitialized: false,

      login: async (data) => {
        const res = await authApi.login(data);
        saveTokens(res);
        set({ user: res.user, isAuthenticated: true });
        return res.user;
      },

      loadSession: async () => {
        if (!getAccessToken()) {
          get().clearSession();
          return;
        }
        const me = await authApi.getMe();
        set({ user: me, isAuthenticated: true });
      },

      clearSession: () => {
        clearStoredTokens();
        set({ user: null, isAuthenticated: false });
      },

      logout: async () => {
        const refreshToken = getRefreshToken();
        if (refreshToken) {
          await authApi.logout(refreshToken).catch(() => undefined);
        }
        get().clearSession();
      },

      setUser: (user) => set({ user }),
      setInitialized: (value) => set({ isInitialized: value })
    }),
    {
      name: appStorage.prefix + STORAGE_KEYS.auth,
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ user: state.user, isAuthenticated: state.isAuthenticated })
    }
  )
);

// ─── Helper ───────────────────────────────────────────────────────────────────

export function getHomePath(user: AccountInfo | null): string {
  return user ? ROLE_HOME[user.role] : ROLE_HOME[ROLE.STUDENT];
}
