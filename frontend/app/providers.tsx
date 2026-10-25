'use client';

import { useEffect } from 'react';

import { App, ConfigProvider, Spin } from 'antd';
import viVN from 'antd/locale/vi_VN';
import dayjs from 'dayjs';
import 'dayjs/locale/vi';

import { RealtimeBridge } from '@/components/RealtimeBridge';
import { antdTheme } from '@/lib/theme/antdTheme';
import { LAYOUT } from '@/lib/theme/tokens';
import { setMessageInstance, useAppStore } from '@/stores/useAppStore';
import { useAuthStore } from '@/stores/useAuthStore';

dayjs.locale('vi');

// ─── Nạp instance message của <App> để dùng ngoài React (axiosClient) ─────────
function MessageBridge() {
  const { message } = App.useApp();

  useEffect(() => {
    setMessageInstance(message);
  }, [message]);

  return null;
}

// ─── Kiểm tra phiên đăng nhập khi mở app ──────────────────────────────────────
function AuthInit() {
  const loadSession = useAuthStore((s) => s.loadSession);
  const clearSession = useAuthStore((s) => s.clearSession);
  const setInitialized = useAuthStore((s) => s.setInitialized);

  useEffect(() => {
    loadSession()
      .catch(() => clearSession())
      .finally(() => setInitialized(true));
  }, [loadSession, clearSession, setInitialized]);

  return null;
}

// ─── Loading toàn cục (axiosClient tự bật/tắt) ────────────────────────────────
function GlobalLoading() {
  const loading = useAppStore((s) => s.loading);

  return <Spin fullscreen spinning={loading} size="large" />;
}

// Thông báo bật lên nằm dưới header (cao 64px) để không che chuông và menu tài khoản.
const NOTIFICATION_CONFIG = { top: LAYOUT.headerHeight + 8 };

// ─── Providers ────────────────────────────────────────────────────────────────
// Theme antd lấy từ design system (lib/theme/antdTheme.ts).

export default function Providers({ children }: { children: React.ReactNode }) {
  return (
    <ConfigProvider locale={viVN} theme={antdTheme}>
      <App notification={NOTIFICATION_CONFIG}>
        <MessageBridge />
        <AuthInit />
        <GlobalLoading />
        <RealtimeBridge />
        {children}
      </App>
    </ConfigProvider>
  );
}
