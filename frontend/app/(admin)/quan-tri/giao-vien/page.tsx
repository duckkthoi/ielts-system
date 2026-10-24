import type { Metadata } from 'next';

import TeachersPage from './index';

export const metadata: Metadata = { title: 'Giáo viên — IELTS Writing' };

export default function Page() {
  return <TeachersPage />;
}
