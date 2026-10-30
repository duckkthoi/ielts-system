import { Suspense } from 'react';

import type { Metadata } from 'next';

import ClassStatsPage from './index';

export const metadata: Metadata = { title: 'Thống kê lớp — AcaWrite' };

export default function Page() {
  return (
    <Suspense>
      <ClassStatsPage />
    </Suspense>
  );
}
