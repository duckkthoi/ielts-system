'use client';

import { useCallback, useEffect } from 'react';

import { App, Button, Modal } from 'antd';
import { Monitor } from 'lucide-react';
import { useRouter } from 'next/navigation';

import { NotificationIcon } from '@/components/NotificationIcon';
import { ROUTES } from '@/lib/constants';
import { connectRealtime, disconnectRealtime, subscribeRealtime } from '@/lib/realtime';
import type { ForceLogoutReason, NotificationDto, SessionMessage } from '@/lib/types/notification';
import { useAuthStore } from '@/stores/useAuthStore';
import { useNotificationStore } from '@/stores/useNotificationStore';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

// ─── Nối WebSocket với phiên đăng nhập ────────────────────────────────────────
// Đặt một lần ở providers: đăng nhập (và đã đổi mật khẩu lần đầu) → kết nối, nạp chuông thông báo;
// đăng xuất → đóng. Server thu hồi phiên (FORCE_LOGOUT) → xoá phiên trên máy và hiện hộp thoại.

const NOTIFICATION_QUEUE = '/user/queue/notifications';
const SESSION_QUEUE = '/user/queue/session';
const TOAST_SECONDS = 6;

const FORCE_LOGOUT_TEXT: Record<ForceLogoutReason, { title: string; body: string }> = {
  NEW_LOGIN: {
    title: 'Tài khoản vừa đăng nhập ở nơi khác',
    body: 'Nếu đây không phải là bạn, hãy đổi mật khẩu sau khi đăng nhập.'
  },
  PASSWORD_CHANGED: {
    title: 'Mật khẩu vừa được thay đổi',
    body: 'Vui lòng đăng nhập lại bằng mật khẩu mới.'
  },
  ACCOUNT_LOCKED: { title: 'Tài khoản đã bị khoá', body: 'Liên hệ quản trị viên nếu bạn cần hỗ trợ.' },
  ADMIN_FORCED: { title: 'Quản trị viên đã đăng xuất phiên này', body: 'Vui lòng đăng nhập lại để tiếp tục.' },
  TOKEN_REUSE: {
    title: 'Phiên đăng nhập không còn an toàn',
    body: 'Hệ thống đã đăng xuất mọi thiết bị. Vui lòng đăng nhập lại và đổi mật khẩu.'
  }
};

function ForceLogoutModal() {
  const router = useRouter();
  const reason = useRealtimeStore((s) => s.forcedLogout);
  const setForcedLogout = useRealtimeStore((s) => s.setForcedLogout);

  const handleLogin = useCallback(() => {
    setForcedLogout(null);
    router.replace(ROUTES.login);
  }, [router, setForcedLogout]);

  const text = reason ? FORCE_LOGOUT_TEXT[reason] : null;
  return (
    <Modal
      open={!!text}
      closable={false}
      mask={{ closable: false }}
      keyboard={false}
      centered
      width={420}
      footer={
        <Button type="primary" block onClick={handleLogin}>
          Đăng nhập lại
        </Button>
      }
    >
      <div className="flex flex-col items-center px-2 pt-3 pb-2 text-center">
        <span className="mb-5 grid size-13 place-items-center rounded-[14px] bg-brand-50 text-brand-600">
          <Monitor size={24} />
        </span>
        <p className="mb-1 text-h3 font-semibold text-neutral-900">{text?.title}</p>
        <p className="text-neutral-500">Bạn đã bị đăng xuất khỏi thiết bị này. {text?.body}</p>
      </div>
    </Modal>
  );
}

export function RealtimeBridge() {
  const router = useRouter();
  const { notification } = App.useApp();
  const userId = useAuthStore((s) => s.user?.id);
  const mustChangePassword = useAuthStore((s) => s.user?.mustChangePassword);
  const clearSession = useAuthStore((s) => s.clearSession);
  const setForcedLogout = useRealtimeStore((s) => s.setForcedLogout);

  const showToast = useCallback(
    (item: NotificationDto) => {
      const key = `notification-${item.id}`;
      notification.open({
        key,
        title: item.title,
        description: item.body,
        icon: <NotificationIcon type={item.type} size={32} />,
        placement: 'topRight',
        duration: TOAST_SECONDS,
        className: item.link ? 'cursor-pointer' : undefined,
        onClick: () => {
          if (!item.link) return;
          notification.destroy(key);
          useNotificationStore
            .getState()
            .markRead(item.id)
            .catch(() => undefined);
          router.push(item.link);
        }
      });
    },
    [notification, router]
  );

  // Kết nối khi có phiên dùng được (server từ chối CONNECT khi còn phải đổi mật khẩu lần đầu).
  useEffect(() => {
    if (!userId || mustChangePassword) return;
    const store = useNotificationStore.getState();
    store.load().catch(() => undefined);

    const offNotification = subscribeRealtime(NOTIFICATION_QUEUE, (payload) => {
      const item = payload as NotificationDto;
      if (useNotificationStore.getState().receive(item)) showToast(item);
    });
    const offSession = subscribeRealtime(SESSION_QUEUE, (payload) => {
      const message = payload as SessionMessage;
      if (message.type !== 'FORCE_LOGOUT') return;
      setForcedLogout(message.reason);
      void disconnectRealtime();
      clearSession();
    });

    connectRealtime({
      // Kết nối lại: lấy bù thông báo bị lỡ trong lúc mất mạng (SD-09), hiện cái mới nhất.
      onReconnected: async () => {
        const missed = await useNotificationStore
          .getState()
          .sync()
          .catch(() => []);
        if (missed[0]) showToast(missed[0]);
      }
    });

    return () => {
      offNotification();
      offSession();
      void disconnectRealtime();
      useNotificationStore.getState().reset();
    };
  }, [userId, mustChangePassword, showToast, setForcedLogout, clearSession]);

  return <ForceLogoutModal />;
}
