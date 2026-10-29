import { Suspense } from 'react';

import type { Metadata } from 'next';

import AssignmentStatsPage from './index';

export const metadata: Metadata = { title: 'Thống kê bài tập — AcaWrite' };

export default function Page() {
  return (
    <Suspense>
      <AssignmentStatsPage />
    </Suspense>
  );
}
