// ─── Ma trận tiến độ (UC-16): áp sự kiện realtime, chọn người cần nhắc ────────
// Hàm thuần (có test). Sự kiện chỉ cập nhật ô / hàng ngay cho giáo viên thấy; số liệu tóm tắt
// vẫn lấy lại từ REST (server là nơi tính), trang tự gọi lại sau một nhịp ngắn.

import type { SubmissionStatus } from '@/lib/theme/status';
import type { ProgressCellDto, ProgressMatrix } from '@/lib/types/stats';

/** `/topic/assignments/{id}/progress` (docs/05 mục B.2). */
export interface ProgressEvent {
  submissionId: number;
  studentId: number;
  partId: number;
  status: SubmissionStatus;
  late: boolean;
  score: number | null;
  at: string;
}

/** `/topic/assignments/{id}/presence`. */
export interface PresenceEvent {
  studentId: number;
  online: boolean;
  viewingPartId: number | null;
  at: string;
}

/** Trạng thái còn phải nộp: chưa làm, đang làm, cần sửa. */
const PENDING: readonly SubmissionStatus[] = ['NOT_STARTED', 'DRAFT', 'REVISION_REQUESTED'];

export const isPending = (cell: Pick<ProgressCellDto, 'status'>) => PENDING.includes(cell.status);

export const cellKey = (studentId: number, partId: number) => `${studentId}-${partId}`;

function nextCell(cell: ProgressCellDto, ev: ProgressEvent): ProgressCellDto {
  const submitted = ev.status === 'SUBMITTED';
  return {
    ...cell,
    submissionId: ev.submissionId,
    status: ev.status,
    // Nộp (kể cả nộp bản sửa) tạo một lần nộp mới.
    currentVersion: submitted ? cell.currentVersion + 1 : cell.currentVersion,
    late: submitted ? ev.late : cell.late,
    overdue: PENDING.includes(ev.status) ? cell.overdue : false,
    // Lần nộp mới chưa có kết quả; lưu nháp lần đầu thì chưa từng có.
    score: ev.status === 'RETURNED' || ev.status === 'REVISION_REQUESTED' ? ev.score : submitted ? null : cell.score
  };
}

/** Sinh viên / phần không có trong ma trận (vd đang lọc lớp khác) → trả nguyên ma trận cũ. */
export function applyProgressEvent(matrix: ProgressMatrix, ev: ProgressEvent): ProgressMatrix {
  let changed = false;
  const rows = matrix.rows.map((row) => {
    if (row.student.id !== ev.studentId) return row;
    const cells = row.cells.map((cell) => {
      if (cell.partId !== ev.partId) return cell;
      changed = true;
      return nextCell(cell, ev);
    });
    return changed ? { ...row, cells } : row;
  });
  return changed ? { ...matrix, rows } : matrix;
}

export function applyPresenceEvent(matrix: ProgressMatrix, ev: PresenceEvent): ProgressMatrix {
  if (!matrix.rows.some((r) => r.student.id === ev.studentId)) return matrix;
  return {
    ...matrix,
    rows: matrix.rows.map((row) =>
      row.student.id === ev.studentId
        ? { ...row, online: ev.online, viewingPartId: ev.online ? ev.viewingPartId : null }
        : row
    )
  };
}

/** Người được nhắc: các hàng đang chọn; không chọn ai → mọi sinh viên còn phần phải nộp (theo `partId` nếu có). */
export function remindTargets(matrix: ProgressMatrix, selected: number[], partId?: number): number[] {
  if (selected.length > 0) return selected;
  return matrix.rows
    .filter((row) => row.cells.some((c) => (partId === undefined || c.partId === partId) && isPending(c)))
    .map((row) => row.student.id);
}

/** Số sinh viên đã nộp (ít nhất một lần) từng phần — ghi trên tiêu đề cột "Task 1 · 28/32". */
export function submittedPerPart(matrix: ProgressMatrix): Record<number, number> {
  const counts: Record<number, number> = {};
  for (const part of matrix.parts) counts[part.id] = 0;
  for (const row of matrix.rows) {
    for (const cell of row.cells) {
      if (cell.currentVersion > 0) counts[cell.partId] = (counts[cell.partId] ?? 0) + 1;
    }
  }
  return counts;
}
