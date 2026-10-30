import type { Metadata } from 'next';

import AssignmentDetailPage from './index';

export const metadata: Metadata = { title: 'Chi tiết bài tập — AcaWrite' };

export default function Page() {
  return <AssignmentDetailPage />;
}
