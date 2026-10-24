import { Suspense } from 'react';

import type { Metadata } from 'next';

import AssignmentEditorPage from './index';

export const metadata: Metadata = { title: 'Soạn bài tập — IELTS Writing' };

export default function Page() {
  return (
    <Suspense>
      <AssignmentEditorPage />
    </Suspense>
  );
}
