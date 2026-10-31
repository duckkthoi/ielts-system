// ─── Thống kê tổng hợp có bộ lọc (H5, docs/05 mục A.9b) ───────────────────────
// Mọi tỉ lệ kèm tử số / mẫu số; `rate` null = mẫu số 0 (không đủ dữ liệu). "Ngày" tính theo `period.zone`.

import type { ErrorStat } from '@/lib/types/stats';

export type AnalyticsScope = 'admin' | 'teacher';

export interface AnalyticsFilter {
  /** YYYY-MM-DD, gồm cả 2 ngày; bỏ trống = 30 ngày gần nhất. */
  from?: string;
  to?: string;
  /** Chỉ admin. */
  teacherId?: number;
  classId?: number;
  assignmentId?: number;
  kind?: 'ESSAY' | 'PAPER';
  status?: 'PUBLISHED' | 'CLOSED';
}

export interface Ratio {
  numerator: number;
  denominator: number;
  rate: number | null;
}

export interface Compare<T> {
  current: T;
  previous: T;
}

export interface AnalyticsOverview {
  period: { from: string; to: string; previousFrom: string; previousTo: string; zone: string };
  users: {
    byRole: Record<string, number>;
    newUsers: Compare<number>;
    active7Days: number;
    active30Days: number;
    activeSessions: number;
    failedLogins: Compare<number>;
  } | null;
  classes: { total: number; withOpenAssignment: number };
  assignments: { kind: string; status: string; count: number }[];
  units: {
    total: number;
    notStarted: number;
    inProgress: number;
    submitted: number;
    revision: number;
    returned: number;
    overdue: number;
    completion: Ratio;
  };
  kpis: {
    submissions: Compare<number>;
    onTime: Compare<Ratio>;
    avgBand: Compare<number | null>;
    avgPaperScore: Compare<number | null>;
  };
  submissionsByDay: { date: string; count: number }[];
  byAssignment: {
    assignmentId: number;
    title: string;
    kind: string;
    notStarted: number;
    inProgress: number;
    submitted: number;
    revision: number;
    returned: number;
    overdue: number;
  }[];
  bandSummary: ScoreSummary;
  bandHistogram: { value: number; count: number }[];
  paperSummary: ScoreSummary;
  paperHistogram: { value: number; count: number }[];
  criteria: { kind: string; name: string; average: number; maxScore: number; count: number }[];
  heatmap: { dow: number; hour: number; count: number }[];
  topErrors: ErrorStat[];
  paper: {
    groups: number;
    studentsWithoutGroup: number;
    updatesInPeriod: number;
    liveComments: number;
    openLiveComments: number;
  };
  generatedAt: string;
}

export interface ScoreSummary {
  count: number;
  mean: number | null;
  median: number | null;
  min: number | null;
  max: number | null;
}

export interface AnalyticsStudentRow {
  studentId: number;
  studentCode: string | null;
  fullName: string;
  classes: string | null;
  assigned: number;
  submitted: number;
  overdue: number;
  revisions: number;
  latestBand: number | null;
  progress: number | null;
  lastActivityAt: string | null;
  alerts: string[];
}
