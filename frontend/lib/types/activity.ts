export type StudentActivityType = 'PAGE_ENTERED' | 'PAGE_LEFT' | 'TAB_HIDDEN' | 'TAB_VISIBLE';

export interface StudentActivityEvent {
  id: number;
  studentId: number;
  studentCode: string;
  fullName: string;
  type: StudentActivityType;
  occurredAt: string;
}
