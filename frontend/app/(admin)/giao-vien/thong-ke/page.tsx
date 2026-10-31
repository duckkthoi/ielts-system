import { Suspense } from 'react';

import type { Metadata } from 'next';

import { StatsTabs } from './StatsTabs';

export const metadata: Metadata = { title: 'Thống kê — AcaWrite' };

export default function Page() {
  return (
    <Suspense>
      <StatsTabs />
    </Suspense>
  );
}
