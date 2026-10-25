// ─── Thông báo + khung tin WebSocket (docs/05 mục A.9, B.2) ───────────────────

export type NotificationType =
  | 'ASSIGNMENT_PUBLISHED'
  | 'SUBMISSION_RECEIVED'
  | 'GRADE_RETURNED'
  | 'REVISION_REQUESTED'
  | 'DEADLINE_REMINDER'
  | 'MANUAL_REMINDER';

/** Cùng một kiểu cho REST và WebSocket (`/user/queue/notifications`). */
export interface NotificationDto {
  id: number;
  type: NotificationType;
  title: string;
  body: string | null;
  /** Đường dẫn trong app để mở khi bấm vào thông báo. */
  link: string | null;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationListParams {
  unreadOnly?: boolean;
  /** ISO 8601 — chỉ lấy thông báo tạo sau thời điểm này (lấy bù sau khi kết nối lại, SD-09). */
  after?: string;
  page?: number;
  size?: number;
}

export type ForceLogoutReason = 'NEW_LOGIN' | 'PASSWORD_CHANGED' | 'ACCOUNT_LOCKED' | 'ADMIN_FORCED' | 'TOKEN_REUSE';

/** Khung tin trên `/user/queue/session` khi phiên này bị server thu hồi. */
export interface SessionMessage {
  type: 'FORCE_LOGOUT';
  reason: ForceLogoutReason;
}
