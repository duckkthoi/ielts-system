import type { AssignmentStatus, SubmissionStatus } from '@/lib/theme/status';
import type { ChecklistKind, CriterionCode, TaskType } from '@/lib/types/assignment';

// ─── Giáo viên chấm bài (docs/05 mục A.7) ─────────────────────────────────────
// Điểm / band là BigDecimal phía server → `number`.

/** DRAFT = đang chấm (sinh viên chưa thấy) · RETURNED = đã trả. */
export type GradeStatus = 'DRAFT' | 'RETURNED';
/** BR-19: lý do chấm 0 cả bài. */
export type ZeroReason = 'NO_ATTEMPT' | 'NOT_ENGLISH' | 'MEMORISED';
/** Nhóm tiêu chí để khớp mã lỗi / nhận xét mẫu (TA, TR → TASK). */
export type CriterionGroup = 'TASK' | 'CC' | 'LR' | 'GRA';
/** Trạng thái lọc hàng đợi (NOT_STARTED / DRAFT không có trong hàng đợi). */
export type QueueStatus = 'SUBMITTED' | 'GRADED' | 'RETURNED' | 'REVISION_REQUESTED';

export interface StudentInfo {
  id: number;
  studentCode: string;
  fullName: string;
}

export interface QueueRow {
  submissionId: number;
  status: SubmissionStatus;
  currentVersion: number;
  student: StudentInfo;
  assignmentId: number;
  assignmentTitle: string;
  partId: number;
  partTitle: string;
  partOrderNo: number;
  taskType: TaskType;
  submittedAt: string;
  late: boolean;
  wordCount: number;
  gradeStatus: GradeStatus | null;
  /** Band phần đã làm tròn; null khi chưa chấm đủ. */
  band: number | null;
}

export interface QueueParams {
  status?: QueueStatus;
  assignmentId?: number;
  partId?: number;
  classId?: number;
  keyword?: string;
  page?: number;
  size?: number;
}

export interface GradingChecklistItem {
  id: number;
  content: string;
  kind: ChecklistKind;
  maxBand: number | null;
}

export interface GradingCriterion {
  id: number;
  code: CriterionCode | null;
  group: CriterionGroup | null;
  name: string;
  description: string | null;
  maxScore: number;
  step: number;
  weight: number;
  checklist: GradingChecklistItem[];
}

export interface ScoreDto {
  criterionId: number;
  score: number | null;
  comment: string | null;
  /** Giới hạn điểm hiện tại (BR-17, BR-18) do server tính. */
  maxAllowed: number;
  limitReason: string | null;
}

export interface InlineCommentDto {
  id: number;
  startOffset: number;
  endOffset: number;
  quote: string;
  comment: string | null;
  errorCode: string | null;
  criterionId: number | null;
  suggestion: string | null;
}

export interface GradeDto {
  id: number;
  versionNo: number;
  status: GradeStatus;
  zeroReason: ZeroReason | null;
  generalComment: string | null;
  rawBand: number | null;
  band: number | null;
  /** Đã chấm đủ mọi tiêu chí — điều kiện để trả bài. */
  complete: boolean;
  graderName: string | null;
  updatedAt: string;
  returnedAt: string | null;
  scores: ScoreDto[];
  checklist: { checklistItemId: number; checked: boolean }[];
  inlineComments: InlineCommentDto[];
}

export interface GradingVersionSummary {
  versionNo: number;
  submittedAt: string;
  wordCount: number;
  late: boolean;
  gradeStatus: GradeStatus | null;
  band: number | null;
}

export interface GradingScreen {
  submission: {
    id: number;
    status: SubmissionStatus;
    currentVersion: number;
    /** Số lần đã yêu cầu sửa (= currentVersion − 1). */
    revisionsUsed: number;
    student: StudentInfo;
    revisionNote: string | null;
    revisionDueAt: string | null;
  };
  assignment: {
    id: number;
    title: string;
    status: AssignmentStatus;
    maxRevisions: number;
    showSuggestionsOnRevision: boolean;
  };
  part: {
    id: number;
    orderNo: number;
    title: string;
    taskType: TaskType;
    /** HTML đã được server lọc. */
    prompt: string;
    imageUrl: string | null;
    minWords: number | null;
    weight: number | null;
  };
  version: { versionNo: number; content: string; wordCount: number; submittedAt: string; late: boolean };
  /** SHORT_ESSAY (≤ 20 từ, BR-18) · UNDER_MIN_WORDS. */
  warnings: { code: string; message: string }[];
  criteria: GradingCriterion[];
  /** null khi version này chưa chấm. */
  grade: GradeDto | null;
  versions: GradingVersionSummary[];
  /** Version mới nhất và chưa trả bài. */
  editable: boolean;
  canReturn: boolean;
  canRequestRevision: boolean;
}

export interface InlineCommentRequest {
  startOffset: number;
  endOffset: number;
  quote?: string | null;
  comment?: string | null;
  errorCode?: string | null;
  criterionId?: number | null;
  suggestion?: string | null;
}

/** PUT …/grade thay toàn bộ kết quả chấm của version (tiêu chí / mục / nhận xét không gửi = chưa chấm / bỏ). */
export interface SaveGradeRequest {
  versionNo: number;
  scores: { criterionId: number; score: number | null; comment: string | null }[];
  checklist: { checklistItemId: number; checked: boolean }[];
  inlineComments: InlineCommentRequest[];
  generalComment: string | null;
  zeroReason: ZeroReason | null;
}
