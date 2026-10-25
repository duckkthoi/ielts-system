import type { AssignmentStatus, SubmissionStatus } from '@/lib/theme/status';
import type { CriterionCode, TaskType } from '@/lib/types/assignment';

// ─── Tiến độ, thống kê, kết quả học tập (docs/05 mục A.6, A.8) ────────────────
// Chỉ tính kết quả đã trả (BR-13). Trung bình giữ 2 chữ số thập phân (không làm tròn kiểu IELTS),
// band hiển thị đã làm tròn .25 / .75; chưa có dữ liệu → null.

export interface StudentRef {
  id: number;
  studentCode: string;
  fullName: string;
}

/** Mã lỗi hay gặp. `criterion` là nhóm tiêu chí (TASK / CC / LR / GRA); `name`, `criterion` null nếu mã riêng đã xoá. */
export interface ErrorStat {
  code: string;
  name: string | null;
  criterion: string | null;
  count: number;
  studentCount: number;
}

export interface BandCount {
  band: number;
  count: number;
}

// ─── Kết quả học tập của một sinh viên (UC-28) ────────────────────────────────

/** Bài có ít nhất một phần đã trả; `band` chỉ có khi `complete`. Sắp theo `returnedAt` tăng dần. */
export interface AssignmentBand {
  assignmentId: number;
  title: string;
  partCount: number;
  returnedParts: number;
  complete: boolean;
  band: number | null;
  returnedAt: string;
}

export interface PartBand {
  assignmentId: number;
  assignmentTitle: string;
  partId: number;
  partTitle: string;
  taskType: TaskType;
  band: number | null;
  returnedAt: string;
}

export interface CriterionCodeAverage {
  code: CriterionCode;
  average: number;
  count: number;
}

export interface StudentStats {
  student: StudentRef;
  submittedCount: number;
  lateCount: number;
  returnedCount: number;
  averageBand: number | null;
  assignments: AssignmentBand[];
  parts: PartBand[];
  criteria: CriterionCodeAverage[];
  errorProfile: ErrorStat[];
}

// ─── Ma trận tiến độ (UC-16) ──────────────────────────────────────────────────

export interface ProgressPart {
  id: number;
  orderNo: number;
  title: string;
  taskType: TaskType;
}

export interface ProgressCellDto {
  partId: number;
  submissionId: number | null;
  status: SubmissionStatus;
  currentVersion: number;
  late: boolean;
  overdue: boolean;
  /** Band của phần, chỉ khi kết quả của lần nộp mới nhất đã trả. */
  score: number | null;
}

export interface ProgressRow {
  student: StudentRef;
  online: boolean;
  viewingPartId: number | null;
  cells: ProgressCellDto[];
}

export interface ProgressSummary {
  students: number;
  completedCount: number;
  completedRate: number;
  lateCount: number;
  overdueCount: number;
}

export interface ProgressMatrix {
  parts: ProgressPart[];
  rows: ProgressRow[];
  summary: ProgressSummary;
}

// ─── Thống kê một bài (UC-19) ─────────────────────────────────────────────────

export interface CriterionAverage {
  criterionId: number;
  code: CriterionCode | null;
  name: string;
  average: number | null;
  count: number;
}

export interface PartStats {
  partId: number;
  orderNo: number;
  title: string;
  taskType: TaskType;
  submittedCount: number;
  lateCount: number;
  onTimeRate: number | null;
  returnedCount: number;
  averageBand: number | null;
  criteria: CriterionAverage[];
  bandDistribution: BandCount[];
}

export interface AssignmentStats {
  students: number;
  perPart: PartStats[];
  overall: { count: number; averageBand: number | null; bandDistribution: BandCount[] };
  topErrors: ErrorStat[];
}

// ─── Thống kê lớp, xếp hạng (FR-65) ───────────────────────────────────────────

export interface ClassAssignmentStats {
  assignmentId: number;
  title: string;
  status: AssignmentStatus;
  dueAt: string | null;
  partCount: number;
  completedCount: number;
  completedRate: number;
  gradedCount: number;
  averageBand: number | null;
}

export interface RankingRow {
  /** Cùng band thì cùng hạng (1, 1, 3); chưa có band → null. */
  rank: number | null;
  student: StudentRef;
  averageBand: number | null;
  gradedAssignments: number;
  submittedParts: number;
  lateParts: number;
}

export interface ClassStats {
  classroom: { id: number; code: string; name: string; memberCount: number };
  averageBand: number | null;
  assignments: ClassAssignmentStats[];
  ranking: RankingRow[];
}
