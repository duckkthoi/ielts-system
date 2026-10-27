import type { Metadata } from 'next';

import TeacherDashboardPage from './index';

export const metadata: Metadata = { title: 'Tổng quan — IELTS Writing' };

export default function Page() {
  return <TeacherDashboardPage />;
}
