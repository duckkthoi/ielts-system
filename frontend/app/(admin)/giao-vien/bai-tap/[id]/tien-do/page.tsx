import type { Metadata } from 'next';

import ProgressPage from './index';

export const metadata: Metadata = { title: 'Tiến độ bài tập — IELTS Writing' };

export default function Page() {
  return <ProgressPage />;
}
