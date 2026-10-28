import type { CriterionDto, CriterionRequest, TaskType } from '@/lib/types/assignment';

// ─── Ngân hàng đề của giáo viên (UC-20 / docs/05 A.5) ───────────────────────

export interface QuestionBankSummary {
  id: number;
  title: string;
  taskType: TaskType;
  imageUrl: string | null;
  minWords: number | null;
  suggestedMinutes: number | null;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface QuestionBankDetail extends QuestionBankSummary {
  /** HTML đã được server lọc. */
  prompt: string;
  criteria: CriterionDto[];
}

export interface QuestionBankRequest {
  title: string;
  taskType: TaskType;
  prompt: string;
  imageUrl?: string | null;
  minWords?: number | null;
  suggestedMinutes?: number | null;
  tags: string[];
  criteria: CriterionRequest[];
}

export interface QuestionBankListParams {
  keyword?: string;
  taskType?: TaskType;
  tag?: string;
  page?: number;
  size?: number;
  sort?: string;
}
