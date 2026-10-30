import { Suspense } from 'react';

import type { Metadata } from 'next';

import GradingPage from './index';

export const metadata: Metadata = { title: 'Chấm bài — AcaWrite' };

export default function Page() {
  return (
    <Suspense>
      <GradingPage />
    </Suspense>
  );
}
