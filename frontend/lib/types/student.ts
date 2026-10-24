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
