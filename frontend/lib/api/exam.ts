import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type { ExamMonitor, ExamPush, ExamSession, ExamTimeline, LiveSnapshot } from '@/lib/types/exam';
import { syncServerClock } from '@/lib/utils/serverClock';

// ─── Chế độ thi + xem trực tiếp (docs/05 mục A.6, A.8) ────────────────────────
// Có `serverTime` → đồng bộ đồng hồ để đếm ngược tới `endsAt` theo giờ server (SD-11).

const URL = {
  current: '/api/student/parts/{partId}/exam',
  start: '/api/student/parts/{partId}/exam/start',
  monitor: '/api/teacher/assignments/{id}/exam-monitor',
  events: '/api/teacher/assignments/{id}/exam-events',
  timeline: '/api/teacher/exam-sessions/{id}/events',
  live: '/api/teacher/parts/{partId}/students/{studentId}/live'
} as const;

// ─── Sinh viên ───

/** Phiên thi của tôi; chưa bắt đầu → null. */
export async function current(partId: number): Promise<ExamSession | null> {
  const res = (await axiosClient.get(formatUrl(URL.current, { partId }), {
    controlLoading: true
  })) as unknown as ApiResponse<ExamSession | null>;
  syncServerClock(res.data?.serverTime);
  return res.data ?? null;
}

/** Bắt đầu tính giờ (BR-23). Gọi lại trả đúng phiên cũ, đồng hồ không đặt lại. */
export async function start(partId: number): Promise<ExamSession> {
  const res = (await axiosClient.post(formatUrl(URL.start, { partId }))) as unknown as ApiResponse<ExamSession>;
  syncServerClock(res.data.serverTime);
  return res.data;
}

// ─── Giáo viên ───

/** Màn giám sát: các phiên thi đã bắt đầu của bài, lọc theo lớp (UC-21). */
export async function monitor(id: number, classId?: number): Promise<ExamMonitor> {
  const res = (await axiosClient.get(formatUrl(URL.monitor, { id }), {
    params: { classId },
    controlLoading: true
  })) as unknown as ApiResponse<ExamMonitor>;
  syncServerClock(res.data.serverTime);
  return res.data;
}

/** 50 sự kiện thi mới nhất của bài (nạp sẵn bảng tin), mới xảy ra trước. */
export async function recentEvents(id: number, classId?: number): Promise<ExamPush[]> {
  const res = (await axiosClient.get(formatUrl(URL.events, { id }), {
    params: { classId },
    controlLoading: true
  })) as unknown as ApiResponse<ExamPush[]>;
  return res.data;
}

export async function timeline(sessionId: number): Promise<ExamTimeline> {
  const res = (await axiosClient.get(formatUrl(URL.timeline, { id: sessionId }), {
    controlLoading: true
  })) as unknown as ApiResponse<ExamTimeline>;
  return res.data;
}

/** Trạng thái ban đầu của màn xem trực tiếp (BR-26); sau đó nghe `/topic/live/…`. */
export async function live(partId: number, studentId: number): Promise<LiveSnapshot> {
  const res = (await axiosClient.get(formatUrl(URL.live, { partId, studentId }), {
    controlLoading: true
  })) as unknown as ApiResponse<LiveSnapshot>;
  return res.data;
}

export const examApi = { current, start, monitor, recentEvents, timeline, live };
