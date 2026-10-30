import type { AssignmentStatus, SubmissionStatus } from '@/lib/theme/status';
import type {
  AssignmentKind,
  ChecklistKind,
  CitationStyle,
  CriterionCode,
  LevelDto,
  TaskType
} from '@/lib/types/assignment';
import type { CslItem } from '@/lib/types/paper';

// ─── Giáo viên chấm bài (docs/05 mục A.7) ─────────────────────────────────────
// Điểm / band là BigDecimal phía server → `number`.

/** DRAFT = đang chấm (sinh viên chưa thấy) · RETURNED = đã trả. */
export type GradeStatus = 'DRAFT' | 'RETURNED';
/** BR-19: lý do chấm 0 cả bài. */
export type ZeroReason = 'NO_ATTEMPT' | 'NOT_ENGLISH' | 'MEMORISED';
/** Kiểu đánh dấu (V9, V10): lỗi (gạch gợn sóng, tính vào thống kê) / lưu ý (vàng) / dùng hay (xanh) / nhận xét (xám). */
export type InlineKind = 'ERROR' | 'HIGHLIGHT' | 'GOOD' | 'COMMENT';
/** Sửa kiểu track changes (V10): thay chữ / xoá chữ / thêm chữ sau đoạn — chữ gốc không đổi. */
export type EditOp = 'REPLACE' | 'DELETE' | 'INSERT';
/** Đánh giá lỗi của lần nộp cũ khi so sánh: đã sửa / chưa sửa / đổi khác gợi ý. */
export type Resolution = 'FIXED' | 'NOT_FIXED' | 'CHANGED';
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
  /** Mức đạt (chỉ bài PAPER — BR-31); rỗng = chấm điểm trực tiếp. */
  levels: LevelDto[];
}

export interface ScoreDto {
  criterionId: number;
  score: number | null;
  comment: string | null;
  /** Giới hạn điểm hiện tại (BR-17, BR-18) do server tính. */
  maxAllowed: number;
  limitReason: string | null;
  /** Bài PAPER: mức đạt đã chọn; null = chấm điểm trực tiếp. */
  levelId: number | null;
}

/** Cộng / trừ riêng một thành viên nhóm (bài PAPER): `score` = điểm cả bài + delta, kẹp [0, 10]. */
export interface AdjustmentDto {
  studentId: number;
  studentName: string;
  delta: number;
  reason: string;
  score: number | null;
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
  kind: InlineKind;
  edit: EditOp | null;
  /** Chỉ đặt trên lần nộp cũ; null = giao diện tự đoán. */
  resolution: Resolution | null;
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
  /** Chỉ bài PAPER. */
  adjustments: AdjustmentDto[];
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
    /** PAPER: chấm thang 10 theo mức đạt (BR-31), không áp quy tắc riêng của IELTS. */
    kind: AssignmentKind;
    /** Kiểu trích dẫn để dựng bài nộp có định dạng; bài luận null. */
    citationStyle: CitationStyle | null;
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
  version: {
    versionNo: number;
    content: string;
    wordCount: number;
    submittedAt: string;
    late: boolean;
    /** Bài PAPER (B25c-2): JSON trình soạn + tài liệu được trích lúc nộp; bài luận null. */
    contentJson: Record<string, unknown> | null;
    references: { id: number; csl: CslItem }[] | null;
  };
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
  /** Bài PAPER: thành viên hiện tại của nhóm (để cộng / trừ riêng); bài luận rỗng. */
  members: StudentInfo[];
}

export interface InlineCommentRequest {
  startOffset: number;
  endOffset: number;
  quote?: string | null;
  comment?: string | null;
  errorCode?: string | null;
  criterionId?: number | null;
  suggestion?: string | null;
  kind?: InlineKind;
  edit?: EditOp | null;
}

// ─── Mã lỗi và ngân hàng nhận xét (BR-21, BR-22) ─────────────────────────────

export interface ErrorCodeDto {
  id: number;
  code: string;
  name: string;
  criterion: CriterionGroup | null;
  description: string | null;
  /** Mã khen (GOOD) — không tính là lỗi. */
  positive: boolean;
  /** Mã của hệ thống (không sửa / xoá được). */
  system: boolean;
}

export interface CommentBankDto {
  id: number;
  criterion: CriterionGroup | null;
  content: string;
  useCount: number;
  lastUsedAt: string | null;
  system: boolean;
}

/** PUT …/grade thay toàn bộ kết quả chấm của version (tiêu chí / mục / nhận xét không gửi = chưa chấm / bỏ). */
export interface SaveGradeRequest {
  versionNo: number;
  scores: { criterionId: number; score: number | null; comment: string | null; levelId?: number | null }[];
  checklist: { checklistItemId: number; checked: boolean }[];
  inlineComments: InlineCommentRequest[];
  generalComment: string | null;
  zeroReason: ZeroReason | null;
  /** Chỉ bài PAPER (PUT = thay toàn bộ): -2…+2, bước 0,25, bắt buộc lý do. */
  memberAdjustments?: { studentId: number; delta: number; reason: string }[];
}
