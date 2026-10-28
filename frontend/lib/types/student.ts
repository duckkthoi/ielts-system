import type { AssignmentKind, CitationStyle, SectionDto } from '@/lib/types/assignment';
import type { AssignmentStatus, SubmissionStatus } from '@/lib/theme/status';
import type { ChecklistKind, CriterionCode, TaskType } from '@/lib/types/assignment';

// ─── Sinh viên: bài được giao, lịch sử nộp, kết quả (docs/05 mục A.8) ─────────
// `status` phía sinh viên: server đã đổi GRADED → SUBMITTED (BR-13).

/** Tab màn "Bài được giao" — một bài có thể thuộc nhiều tab. */
export type StudentTab = 'TODO' | 'DUE_SOON' | 'OVERDUE' | 'SUBMITTED' | 'REVISION';

export interface StudentPartStatus {
  partId: number;
  orderNo: number;
  title: string;
  taskType: TaskType;
  status: SubmissionStatus;
  currentVersion: number;
  late: boolean;
  /** Hạn áp dụng cho phần: hạn sửa > hạn riêng của phần > hạn chung. */
  effectiveDueAt: string | null;
  canSubmit: boolean;
}

export interface StudentAssignmentItem {
  id: number;
  title: string;
  teacherName: string;
  status: AssignmentStatus;
  openAt: string | null;
  dueAt: string | null;
  /** Hạn gần nhất của phần còn phải làm; null = không còn hạn nào. */
  nextDueAt: string | null;
  tabs: StudentTab[];
  parts: StudentPartStatus[];
}

export interface StudentAssignmentList {
  counts: Record<StudentTab, number>;
  items: StudentAssignmentItem[];
  serverTime: string;
}

export interface StudentChecklistItem {
  content: string;
  kind: ChecklistKind;
  maxBand: number | null;
}

export interface StudentCriterion {
  code: CriterionCode | null;
  name: string;
  description: string | null;
  maxScore: number;
  step: number;
  checklist: StudentChecklistItem[];
}

export interface StudentPartDetail extends StudentPartStatus {
  /** HTML đã được server lọc — chỉ trường này được hiển thị bằng dangerouslySetInnerHTML. */
  prompt: string;
  imageUrl: string | null;
  minWords: number | null;
  suggestedMinutes: number | null;
  revisionNote: string | null;
  revisionDueAt: string | null;
  criteria: StudentCriterion[];
  /** Chế độ thi (Mức 3): phải bấm "Bắt đầu" mới soạn được, đồng hồ cố định từ lúc đó (BR-23). */
  examMode: boolean;
  examDurationMinutes: number | null;
  /** Chỉ bài PAPER: khung mục của giai đoạn. */
  sections: SectionDto[];
}

export interface StudentAssignmentDetail {
  id: number;
  title: string;
  description: string | null;
  teacherName: string;
  status: AssignmentStatus;
  openAt: string | null;
  dueAt: string | null;
  allowLate: boolean;
  maxRevisions: number;
  tabs: StudentTab[];
  parts: StudentPartDetail[];
  serverTime: string;
  kind: AssignmentKind;
  citationStyle: CitationStyle | null;
  maxGroupSize: number | null;
  groupLockAt: string | null;
}

/** Một lần nộp; `band`, `returnedAt` chỉ có khi version đó đã được trả bài. */
export interface VersionSummary {
  versionNo: number;
  submittedAt: string;
  wordCount: number;
  late: boolean;
  returned: boolean;
  band: number | null;
  returnedAt: string | null;
}

export interface PartResult {
  partId: number;
  orderNo: number;
  title: string;
  taskType: TaskType;
  weight: number | null;
  /** Lần nộp gần nhất đã được trả bài; null nếu chưa có. */
  versionNo: number | null;
  band: number | null;
}

/** Band cả bài chỉ có khi mọi phần đã có kết quả (BR-12). */
export interface AssignmentResult {
  assignmentId: number;
  title: string;
  parts: PartResult[];
  complete: boolean;
  band: number | null;
  note: string | null;
}

// ─── Làm bài: nháp, nộp (SD-06, SD-07) ────────────────────────────────────────

/** DRAFT = nháp đang lưu · LAST_VERSION = đang "Cần sửa", nạp sẵn bản nộp gần nhất · EMPTY = chưa có gì. */
export type DraftSource = 'DRAFT' | 'LAST_VERSION' | 'EMPTY';

export interface DraftResponse {
  content: string;
  source: DraftSource;
  savedAt: string | null;
  wordCount: number;
  status: SubmissionStatus;
  currentVersion: number;
  /** Gửi lại ở lần lưu sau để phát hiện tab / máy khác đã lưu (khoá lạc quan). */
  draftVersion: number;
  /** Kênh chat `submission-{id}` để nhận bình luận trực tiếp của giáo viên (BR-26); chưa lưu lần nào → null. */
  submissionId: number | null;
}

export interface SaveDraftResponse {
  savedAt: string;
  wordCount: number;
  draftVersion: number;
  submissionId: number;
}

/** `data` của lỗi 409 DRAFT_CONFLICT. */
export interface DraftConflict {
  serverContent: string | null;
  draftVersion: number;
  savedAt: string | null;
}

export interface SubmitResponse {
  versionNo: number;
  submittedAt: string;
  late: boolean;
  wordCount: number;
}

// ─── Một lần nộp + kết quả chấm (chỉ khi đã trả bài, BR-13) ───────────────────

export interface ResultChecklistMark {
  content: string;
  kind: ChecklistKind;
  maxBand: number | null;
  checked: boolean;
}

export interface ResultCriterion {
  criterionId: number;
  code: CriterionCode | null;
  name: string;
  maxScore: number;
  score: number | null;
  comment: string | null;
  limitedTo: number | null;
  limitReason: string | null;
  checklist: ResultChecklistMark[];
}

export interface StudentInlineComment {
  startOffset: number;
  endOffset: number;
  quote: string;
  comment: string | null;
  errorCode: string | null;
  errorName: string | null;
  criterionCode: CriterionCode | null;
  criterionName: string | null;
  /** null khi bị ẩn (BR-21) hoặc giáo viên không ghi. */
  suggestion: string | null;
  kind: 'ERROR' | 'HIGHLIGHT' | 'GOOD' | 'COMMENT';
  /** null khi không sửa, hoặc bị ẩn vì bài đang "Cần sửa" (BR-21). */
  edit: 'REPLACE' | 'DELETE' | 'INSERT' | null;
  /** Giáo viên đánh giá lỗi này đã sửa ở lần nộp sau chưa (null = chưa đánh giá). */
  resolution: 'FIXED' | 'NOT_FIXED' | 'CHANGED' | null;
}

export interface ErrorCount {
  criterionCode: CriterionCode | null;
  criterionName: string | null;
  errorCode: string;
  errorName: string;
  count: number;
}

export interface VersionResult {
  band: number | null;
  zeroReason: 'NO_ATTEMPT' | 'NOT_ENGLISH' | 'MEMORISED' | null;
  generalComment: string | null;
  graderName: string | null;
  returnedAt: string | null;
  suggestionsHidden: boolean;
  criteria: ResultCriterion[];
  inlineComments: StudentInlineComment[];
  errorSummary: ErrorCount[];
}

export interface VersionDetail {
  versionNo: number;
  submittedAt: string;
  wordCount: number;
  late: boolean;
  content: string;
  /** null khi chưa được trả bài. */
  result: VersionResult | null;
}
