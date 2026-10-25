import type { Metadata } from 'next';

import AssignmentDetailPage from './index';

export const metadata: Metadata = { title: 'Chi tiết bài tập — IELTS Writing' };

export default function Page() {
  return <AssignmentDetailPage />;
}
