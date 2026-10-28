import type { AssignmentStatus } from '@/lib/theme/status';

// ─── Bài tập (docs/05 mục A.5) ────────────────────────────────────────────────
// Điểm / trọng số là số thập phân (BigDecimal phía server) → `number`.

export type TaskType = 'WRITING_TASK1' | 'WRITING_TASK2' | 'CUSTOM';
/** ESSAY = bài luận IELTS (cá nhân, band); PAPER = paper học thuật theo nhóm, thang 10 (Mức 4, docs/12). */
export type AssignmentKind = 'ESSAY' | 'PAPER';
export type CitationStyle = 'IEEE' | 'APA';
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
  /** Mức đạt (chỉ bài PAPER — BR-31); bài luận rỗng. */
  levels: LevelDto[];
}

/** Một mức đạt của tiêu chí bài PAPER: khoảng điểm [minScore, maxScore] + mô tả (B25b). */
export interface LevelDto {
  id: number;
  label: string;
  minScore: number;
  maxScore: number;
  descriptor: string | null;
  orderNo: number;
}

export interface LevelRequest {
  label: string;
  minScore: number;
  maxScore: number;
  descriptor?: string | null;
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
  /** Chế độ thi tính giờ (Mức 3, BR-23). */
  examMode: boolean;
  examDurationMinutes: number | null;
  /** Chỉ bài PAPER: khung mục của giai đoạn (BR-29). */
  sections: SectionDto[];
}

export interface SectionDto {
  id: number;
  sectionKey: string;
  title: string;
  required: boolean;
  minWords: number | null;
  maxWords: number | null;
  orderNo: number;
}

export interface SectionRequest {
  sectionKey?: string | null;
  title: string;
  required?: boolean;
  minWords?: number | null;
  maxWords?: number | null;
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
  kind: AssignmentKind;
  citationStyle: CitationStyle | null;
  maxGroupSize: number | null;
  groupLockAt: string | null;
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
  /** Chỉ bài PAPER (BR-27). */
  groupLockAt?: string;
  maxGroupSize?: number;
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
  levels?: LevelRequest[];
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
  examMode?: boolean;
  /** Bắt buộc khi `examMode`; bỏ trống → server lấy `suggestedMinutes`. */
  examDurationMinutes?: number | null;
  sections?: SectionRequest[];
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
  kind?: AssignmentKind;
  citationStyle?: CitationStyle | null;
  maxGroupSize?: number | null;
  groupLockAt?: string | null;
}

export interface RubricTemplateDto {
  id: number;
  name: string;
  taskType: TaskType;
  description: string | null;
  system: boolean;
  criteria: CriterionDto[];
}

// ─── Bảng mô tả band thu gọn (GET /api/teacher/rubric-templates/descriptors) ──
// Chỉ là diễn giải ngắn tiếng Việt, không phải văn bản chính thức (docs/08 mục 5.4).

export interface BandDescriptors {
  source: { title: string; url: string };
  note: string;
  /** Theo mã tiêu chí; `bands` theo band dạng chuỗi "9" → "0". */
  criteria: Partial<Record<CriterionCode, { name: string; taskTypes: TaskType[]; bands: Record<string, string> }>>;
}
