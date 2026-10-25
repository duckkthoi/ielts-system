import type { Metadata } from 'next';

import AssignmentListPage from './index';

export const metadata: Metadata = { title: 'Bài tập — IELTS Writing' };

export default function Page() {
  return <AssignmentListPage />;
}
