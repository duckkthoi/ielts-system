import { Suspense } from 'react';

import type { Metadata } from 'next';

import StudentAssignmentDetailPage from './index';

export const metadata: Metadata = { title: 'Chi tiết bài tập — AcaWrite' };

export default function Page() {
  return (
    <Suspense>
      <StudentAssignmentDetailPage />
    </Suspense>
  );
}
