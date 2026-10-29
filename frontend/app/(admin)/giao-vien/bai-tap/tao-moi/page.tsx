import { Suspense } from 'react';

import type { Metadata } from 'next';

import AssignmentEditorPage from './index';

export const metadata: Metadata = { title: 'Soạn bài tập — AcaWrite' };

export default function Page() {
  return (
    <Suspense>
      <AssignmentEditorPage />
    </Suspense>
  );
}
