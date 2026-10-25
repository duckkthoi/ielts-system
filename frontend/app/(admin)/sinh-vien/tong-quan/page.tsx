import { Suspense } from 'react';

import type { Metadata } from 'next';

import StudentAssignmentsPage from './index';

export const metadata: Metadata = { title: 'Bài được giao — IELTS Writing' };

export default function Page() {
  return (
    <Suspense>
      <StudentAssignmentsPage />
    </Suspense>
  );
}
