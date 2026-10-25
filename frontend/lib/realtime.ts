'use client';

import { Client, ReconnectionTimeMode } from '@stomp/stompjs';
import type { IMessage, StompSubscription } from '@stomp/stompjs';

import { authApi } from '@/lib/api/auth';
import { refreshAccessToken } from '@/lib/axiosClient';
import { getAccessToken } from '@/lib/tokens';
import { useRealtimeStore } from '@/stores/useRealtimeStore';

// ─── Một STOMP client cho cả app (docs/05 phần B, docs/06 mục 6, SD-09) ───────
// - Kết nối sau khi đăng nhập (RealtimeBridge), đóng khi đăng xuất / bị thu hồi phiên.
// - Mất kết nối: stompjs tự thử lại theo backoff 1s → 2s → 4s … tối đa 30s.
// - Trước mỗi lần kết nối: token sắp hết hạn thì refresh qua REST.
// - Kết nối lại thành công: SUBSCRIBE lại mọi kênh rồi gọi `onReconnected` (lấy bù thông báo bị lỡ).

type Handler = (payload: unknown) => void;

const HEARTBEAT_MS = 10_000;
/** Còn ít hơn chừng này thì coi token sắp hết hạn, refresh trước khi CONNECT. */
const TOKEN_MIN_TTL_MS = 30_000;

let client: Client | null = null;
let connectedOnce = false;
let onReconnected: (() => Promise<void>) | null = null;
/** Kênh → các handler đang nghe. Giữ qua các lần kết nối lại để SUBSCRIBE lại. */
const handlers = new Map<string, Set<Handler>>();
const active = new Map<string, StompSubscription>();

/** Thời điểm hết hạn (ms) đọc từ phần payload của JWT; đọc không được thì null. */
function tokenExpiresAt(token: string): number | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))) as { exp?: number };
    return payload.exp ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

async function freshToken(): Promise<string | null> {
  const token = getAccessToken();
  if (!token) return null;
  const exp = tokenExpiresAt(token);
  if (exp !== null && exp - Date.now() < TOKEN_MIN_TTL_MS) return refreshAccessToken();
  return token;
}

function dispatch(destination: string, message: IMessage) {
  let payload: unknown;
  try {
    payload = JSON.parse(message.body);
  } catch {
    return;
  }
  handlers.get(destination)?.forEach((handler) => handler(payload));
}

function subscribeOnServer(destination: string) {
  if (!client?.connected || active.has(destination)) return;
  active.set(
    destination,
    client.subscribe(destination, (message) => dispatch(destination, message))
  );
}

/** Nghe một kênh; trả hàm huỷ. Gọi lúc chưa kết nối cũng được — sẽ SUBSCRIBE khi kết nối xong. */
export function subscribeRealtime(destination: string, handler: Handler): () => void {
  let set = handlers.get(destination);
  if (!set) {
    set = new Set();
    handlers.set(destination, set);
  }
  set.add(handler);
  subscribeOnServer(destination);

  return () => {
    const current = handlers.get(destination);
    current?.delete(handler);
    if (current && current.size === 0) {
      handlers.delete(destination);
      active.get(destination)?.unsubscribe();
      active.delete(destination);
    }
  };
}

/** Gửi một lệnh STOMP (vd presence, chat). Chưa kết nối thì bỏ qua và trả false. */
export function publishRealtime(destination: string, body: unknown): boolean {
  if (!client?.connected) return false;
  client.publish({ destination, body: JSON.stringify(body) });
  return true;
}

/** Để trống NEXT_PUBLIC_WS_URL → cùng origin với trang (/ws, đi qua proxy của Next hoặc nginx). */
function wsUrl(): string {
  if (process.env.NEXT_PUBLIC_WS_URL) return process.env.NEXT_PUBLIC_WS_URL;
  return `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`;
}

export function connectRealtime(options: { onReconnected: () => Promise<void> }) {
  if (client) return;
  onReconnected = options.onReconnected;
  const { setStatus } = useRealtimeStore.getState();
  setStatus('CONNECTING');

  client = new Client({
    brokerURL: wsUrl(),
    heartbeatIncoming: HEARTBEAT_MS,
    heartbeatOutgoing: HEARTBEAT_MS,
    reconnectDelay: 1_000,
    reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
    maxReconnectDelay: 30_000,

    beforeConnect: async (c) => {
      const token = await freshToken();
      if (!token) {
        // Không còn phiên: axiosClient đã chuyển về trang đăng nhập khi refresh thất bại.
        void disconnectRealtime();
        return;
      }
      c.connectHeaders = { Authorization: `Bearer ${token}` };
    },

    onConnect: () => {
      active.clear();
      handlers.forEach((_, destination) => subscribeOnServer(destination));
      if (!connectedOnce) {
        connectedOnce = true;
        setStatus('CONNECTED');
        return;
      }
      setStatus('SYNCING');
      void (onReconnected?.() ?? Promise.resolve()).finally(() => {
        if (useRealtimeStore.getState().status === 'SYNCING') setStatus('CONNECTED');
      });
    },

    onWebSocketClose: () => {
      active.clear();
      if (!client?.active) return;
      const { status, attempt } = useRealtimeStore.getState();
      // Chưa từng kết nối được thì vẫn là "đang kết nối"; đã kết nối rồi mới là "mất kết nối".
      setStatus(connectedOnce ? 'RECONNECTING' : 'CONNECTING', status === 'RECONNECTING' ? attempt + 1 : 1);
    },

    // Server từ chối (token bị thu hồi, phiên hết hạn…): gọi một API REST để axiosClient tự refresh hoặc về đăng nhập.
    onStompError: () => {
      authApi.getMe().catch(() => undefined);
    }
  });
  client.activate();
}

export async function disconnectRealtime() {
  const current = client;
  client = null;
  connectedOnce = false;
  onReconnected = null;
  active.clear();
  useRealtimeStore.getState().setStatus('DISCONNECTED');
  await current?.deactivate();
}
