import type { Metadata } from 'next';

import ClassListPage from './index';

export const metadata: Metadata = { title: 'Lớp học — IELTS Writing' };

export default function Page() {
  return <ClassListPage />;
}
