'use client';

import type { MessageInstance } from 'antd/es/message/interface';
import { create } from 'zustand';

// ─── Store ────────────────────────────────────────────────────────────────────

interface AppState {
  /** Loading toàn cục (spinner full màn hình) — do axiosClient tự bật/tắt qua interceptor. */
  loading: boolean;
  setLoading: (loading: boolean) => void;
}

export const useAppStore = create<AppState>()((set) => ({
  loading: false,
  setLoading: (loading) => set({ loading })
}));

// ─── message dùng được ngoài React ────────────────────────────────────────────
// `MessageBridge` (trong providers.tsx) nạp instance của `App.useApp().message` vào đây,
// để axiosClient và các hàm thuần gọi được toast theo đúng theme.

let messageInstance: MessageInstance | null = null;

export function setMessageInstance(instance: MessageInstance) {
  messageInstance = instance;
}

export function getMessage(): MessageInstance | null {
  return messageInstance;
}
