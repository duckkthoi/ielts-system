import type { Metadata } from 'next';

import StudentResultsPage from './index';

export const metadata: Metadata = { title: 'Kết quả học tập — IELTS Writing' };

export default function Page() {
  return <StudentResultsPage />;
}
