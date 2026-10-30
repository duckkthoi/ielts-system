import { Suspense } from 'react';

import type { Metadata } from 'next';

import StudentResultPage from './index';

export const metadata: Metadata = { title: 'Kết quả bài làm — AcaWrite' };

export default function Page() {
  return (
    <Suspense>
      <StudentResultPage />
    </Suspense>
  );
}
