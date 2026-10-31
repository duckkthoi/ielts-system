import { Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';

import type { AuditAction, AuditLog } from '@/lib/types/admin';
import { formatDateTime } from '@/lib/utils/date';

export const PAGE_SIZE = 30;

/** Nhãn tiếng Việt + màu Tag theo hành động (docs/04 mục audit_logs). */
export const AUDIT_ACTION_META: Record<AuditAction, { label: string; color: string }> = {
  LOGIN: { label: 'Đăng nhập', color: 'default' },
  LOGIN_FAILED: { label: 'Đăng nhập sai', color: 'warning' },
  LOGIN_BLOCKED: { label: 'Chặn đăng nhập', color: 'error' },
  CREATE_TEACHER: { label: 'Tạo giáo viên', color: 'processing' },
  LOCK_USER: { label: 'Khoá tài khoản', color: 'error' },
  UNLOCK_USER: { label: 'Mở khoá', color: 'success' },
  RESET_PASSWORD: { label: 'Đặt lại mật khẩu', color: 'warning' },
  REVOKE_SESSION: { label: 'Thu hồi phiên', color: 'warning' },
  REVOKE_USER_SESSIONS: { label: 'Thu hồi mọi phiên', color: 'warning' },
  CLEANUP_SESSIONS: { label: 'Dọn phiên hết hạn', color: 'default' },
  IMPORT_STUDENTS: { label: 'Import sinh viên', color: 'processing' },
  TCP_IMPORT: { label: 'Import qua TCP', color: 'processing' },
  PUBLISH_ASSIGNMENT: { label: 'Phát hành bài', color: 'processing' },
  RETURN_GRADE: { label: 'Trả bài', color: 'success' },
  REQUEST_REVISION: { label: 'Yêu cầu sửa', color: 'warning' }
};

export const ACTION_OPTIONS = (Object.keys(AUDIT_ACTION_META) as AuditAction[]).map((action) => ({
  value: action,
  label: AUDIT_ACTION_META[action].label
}));

const TARGET_LABEL: Record<string, string> = {
  USER: 'Tài khoản',
  USER_SESSION: 'Phiên đăng nhập',
  CLASSROOM: 'Lớp',
  ASSIGNMENT: 'Bài tập',
  SUBMISSION: 'Bài nộp'
};

export const columns: ColumnsType<AuditLog> = [
  {
    title: 'Thời gian',
    dataIndex: 'createdAt',
    width: 150,
    render: (value: string) => <span className="tabular-nums">{formatDateTime(value)}</span>
  },
  {
    title: 'Người thực hiện',
    key: 'actor',
    render: (_, row) =>
      row.actorUsername ? (
        <span>
          <span className="block font-medium text-neutral-900">{row.actorName ?? row.actorUsername}</span>
          <span className="font-mono text-caption text-neutral-500">{row.actorUsername}</span>
        </span>
      ) : (
        <span className="text-neutral-500">Không xác định</span>
      )
  },
  {
    title: 'Hành động',
    dataIndex: 'action',
    render: (action: AuditAction) => {
      const meta = AUDIT_ACTION_META[action];
      return <Tag color={meta?.color}>{meta?.label ?? action}</Tag>;
    }
  },
  {
    title: 'Đối tượng',
    key: 'target',
    responsive: ['md'],
    render: (_, row) =>
      row.targetType
        ? `${TARGET_LABEL[row.targetType] ?? row.targetType}${row.targetId ? ` #${row.targetId}` : ''}`
        : '—'
  },
  {
    title: 'IP',
    dataIndex: 'ip',
    render: (value: string | null) => <span className="font-mono text-small">{value ?? '—'}</span>
  }
];
