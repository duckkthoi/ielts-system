import type { AssignmentStatus } from '@/lib/theme/status';

// ─── Bài tập (docs/05 mục A.5) ────────────────────────────────────────────────
// Điểm / trọng số là số thập phân (BigDecimal phía server) → `number`.

export type TaskType = 'WRITING_TASK1' | 'WRITING_TASK2' | 'CUSTOM';
export type CriterionCode = 'TA' | 'TR' | 'CC' | 'LR' | 'GRA';
/** CHECK = đặc điểm cần có; LIMIT = đặc điểm tiêu cực giới hạn band (BR-16). */
export type ChecklistKind = 'CHECK' | 'LIMIT';

export interface AssignmentSummary {
  id: number;
  title: string;
  status: AssignmentStatus;
  openAt: string | null;
  dueAt: string | null;
  partCount: number;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
  recipientCount: number;
  /** Số người nhận đã nộp đủ mọi phần. */
  completedCount: number;
  /** Số bài nộp (theo phần) đang chờ chấm. */
  pendingGradingCount: number;
}

export interface ChecklistItemDto {
  id: number;
  content: string;
  kind: ChecklistKind;
  maxBand: number | null;
  orderNo: number;
}

export interface CriterionDto {
  id: number;
  code: CriterionCode | null;
  name: string;
  description: string | null;
  maxScore: number;
  step: number;
  weight: number;
  orderNo: number;
  checklist: ChecklistItemDto[];
}

export interface PartDto {
  id: number;
  orderNo: number;
  title: string;
  taskType: TaskType;
  /** HTML đã được server lọc — chỉ trường này được hiển thị bằng dangerouslySetInnerHTML. */
  prompt: string;
  imageUrl: string | null;
  minWords: number | null;
  suggestedMinutes: number | null;
  partDueAt: string | null;
  weight: number | null;
  criteria: CriterionDto[];
}

export interface AssignmentDetail {
  id: number;
  title: string;
  description: string | null;
  status: AssignmentStatus;
  openAt: string | null;
  dueAt: string | null;
  allowLate: boolean;
  maxRevisions: number;
  showSuggestionsOnRevision: boolean;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
  parts: PartDto[];
}

export interface AssignmentListParams {
  status?: AssignmentStatus;
  keyword?: string;
  page?: number;
  size?: number;
}

/** PATCH …/settings — trường bỏ trống = giữ nguyên; `partDueAts[partId] = null` = bỏ hạn riêng. */
export interface AssignmentSettingsRequest {
  title?: string;
  description?: string;
  dueAt?: string;
  allowLate?: boolean;
  maxRevisions?: number;
  showSuggestionsOnRevision?: boolean;
  partDueAts?: Record<number, string | null>;
}

export interface TargetsRequest {
  classIds: number[];
  studentIds: number[];
}

export interface Targets {
  classes: { id: number; code: string; name: string; memberCount: number }[];
  students: { id: number; studentCode: string; fullName: string }[];
  recipientCount: number;
}

export interface StudentOption {
  id: number;
  studentCode: string;
  fullName: string;
}

// ─── Body tạo / sửa bài nháp (dùng ở trình soạn) ──────────────────────────────

export interface ChecklistItemRequest {
  content: string;
  kind: ChecklistKind;
  maxBand?: number | null;
}

export interface CriterionRequest {
  code?: CriterionCode | null;
  name: string;
  description?: string | null;
  maxScore: number;
  step: number;
  weight: number;
  checklist: ChecklistItemRequest[];
}

export interface PartRequest {
  title: string;
  taskType: TaskType;
  prompt: string;
  imageUrl?: string | null;
  minWords?: number | null;
  suggestedMinutes?: number | null;
  partDueAt?: string | null;
  weight?: number | null;
  criteria: CriterionRequest[];
}

export interface AssignmentRequest {
  title: string;
  description?: string | null;
  openAt?: string | null;
  dueAt?: string | null;
  allowLate?: boolean;
  maxRevisions?: number;
  showSuggestionsOnRevision?: boolean;
  parts: PartRequest[];
}

export interface RubricTemplateDto {
  id: number;
  name: string;
  taskType: TaskType;
  description: string | null;
  system: boolean;
  criteria: CriterionDto[];
}
