'use client';

import { create } from 'zustand';

import type { ForceLogoutReason } from '@/lib/types/notification';

// ─── Trạng thái kết nối WebSocket (docs/06 mục 6) ─────────────────────────────
// Do lib/realtime.ts cập nhật; giao diện (banner, chấm trạng thái, hộp thoại bị đăng xuất) chỉ đọc.

export type ConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING' | 'SYNCING';

interface RealtimeState {
  status: ConnectionStatus;
  /** Số lần thử kết nối lại liên tiếp (hiện trên banner "lần n"); về 0 khi kết nối được. */
  attempt: number;
  /** Có giá trị khi server thu hồi phiên này (FORCE_LOGOUT) → hiện hộp thoại. */
  forcedLogout: ForceLogoutReason | null;
  setStatus: (status: ConnectionStatus, attempt?: number) => void;
  setForcedLogout: (reason: ForceLogoutReason | null) => void;
}

export const useRealtimeStore = create<RealtimeState>()((set) => ({
  status: 'DISCONNECTED',
  attempt: 0,
  forcedLogout: null,
  setStatus: (status, attempt = 0) => set({ status, attempt }),
  setForcedLogout: (reason) => set({ forcedLogout: reason })
}));
