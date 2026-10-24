import axiosClient from '@/lib/axiosClient';
import type { ApiResponse } from '@/lib/types/api';
import { bestClockOffset, setClockOffset } from '@/lib/utils/serverClock';
import type { ClockSample } from '@/lib/utils/serverClock';

// ─── Đồng bộ giờ với server (SD-11) ───────────────────────────────────────────
// Gọi GET /api/time 3 lần, lấy lần đi về nhanh nhất. Chỉ để hiển thị đồng hồ đếm ngược —
// quyết định nộp muộn luôn ở server. Lỗi mạng thì giữ độ lệch cũ, không báo lỗi.

const SAMPLES = 3;

export async function syncClock(): Promise<void> {
  const samples: ClockSample[] = [];
  for (let i = 0; i < SAMPLES; i++) {
    const sentAt = Date.now();
    try {
      const res = (await axiosClient.get('/api/time', {
        controlLoading: true,
        toastError: false
      })) as unknown as ApiResponse<{ serverTime: string }>;
      samples.push({ serverTime: res.data.serverTime, sentAt, receivedAt: Date.now() });
    } catch {
      // bỏ qua mẫu lỗi
    }
  }
  const offset = bestClockOffset(samples);
  if (offset !== null) setClockOffset(offset);
}

export const timeApi = { syncClock };
