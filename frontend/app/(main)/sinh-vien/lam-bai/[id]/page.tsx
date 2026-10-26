import { Suspense } from 'react';

import type { Metadata } from 'next';

import WritingPage from './index';

export const metadata: Metadata = { title: 'Làm bài — IELTS Writing' };

export default function Page() {
  return (
    <Suspense>
      <WritingPage />
    </Suspense>
  );
}
