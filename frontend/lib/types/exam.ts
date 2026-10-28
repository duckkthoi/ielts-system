import type { SubmissionStatus } from '@/lib/theme/status';

// ─── Chế độ thi (docs/05 mục A.6, A.8, B.2; docs/06 mục 8) ─────────────────────

/** Tính từ giờ server: chưa nộp + còn giờ / hết giờ; đã nộp trong giờ / sau giờ. */
export type ExamStatus = 'IN_PROGRESS' | 'OVERTIME' | 'FINISHED' | 'FINISHED_OVERTIME';

export type ExamEventType =
  | 'STARTED'
  | 'TAB_HIDDEN'
  | 'TAB_VISIBLE'
  | 'WINDOW_BLUR'
  | 'WINDOW_FOCUS'
  | 'PASTE'
  | 'DISCONNECT'
  | 'RECONNECT'
  | 'OVERTIME'
  | 'SUBMITTED';

/** Phiên thi của tôi (`GET / POST /api/student/parts/{partId}/exam[/start]`). */
export interface ExamSession {
  sessionId: number;
  partId: number;
  startedAt: string;
  endsAt: string;
  finishedAt: string | null;
  status: ExamStatus;
  serverTime: string;
}

// ─── Giáo viên: màn giám sát (A.6) ────────────────────────────────────────────

export interface ExamPartRef {
  id: number;
  title: string;
  durationMinutes: number | null;
}

export interface MonitorRow {
  sessionId: number;
  partId: number;
  partTitle: string;
  studentId: number;
  studentCode: string;
  studentName: string;
  startedAt: string;
  endsAt: string;
  finishedAt: string | null;
  status: ExamStatus;
  online: boolean;
  tabHiddenCount: number;
  pasteCount: number;
  disconnectCount: number;
  lastEventAt: string | null;
}

export interface ExamMonitor {
  assignmentId: number;
  title: string;
  parts: ExamPartRef[];
  rows: MonitorRow[];
  serverTime: string;
}

export interface ExamEventDto {
  id: number;
  type: ExamEventType;
  pasteChars: number | null;
  occurredAt: string;
  /** Máy sinh viên gửi bù khi có mạng lại. */
  reportedLate: boolean;
}

export interface ExamTimeline {
  session: MonitorRow;
  events: ExamEventDto[];
}

/** `/topic/assignments/{id}/exam`: một sự kiện vừa ghi, `status` là trạng thái phiên ngay sau đó. */
export interface ExamPush {
  sessionId: number;
  partId: number;
  studentId: number;
  studentName: string;
  type: ExamEventType;
  pasteChars: number | null;
  status: ExamStatus;
  at: string;
  /** Máy sinh viên gửi bù khi có mạng lại (`at` = lúc xảy ra). */
  reportedLate: boolean;
}

// ─── Xem trực tiếp bài đang gõ (BR-26) ────────────────────────────────────────

export type LiveSource = 'LIVE' | 'DRAFT' | 'EMPTY' | 'NOT_WORKING';

export interface LiveSnapshot {
  partId: number;
  studentId: number;
  /** Kênh chat `submission-{id}` để bình luận trực tiếp; sinh viên chưa lưu lần nào → null. */
  submissionId: number | null;
  status: SubmissionStatus;
  source: LiveSource;
  content: string;
  wordCount: number;
  updatedAt: string | null;
  online: boolean;
  viewingPartId: number | null;
}

/** `/topic/live/parts/{partId}/students/{studentId}`. */
export interface LiveEvent {
  type: 'CONTENT' | 'SUBMITTED';
  partId: number;
  studentId: number;
  content: string;
  wordCount: number;
  at: string;
}
