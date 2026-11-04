import type { Metadata } from 'next';

import StudentClassesPage from './index';

export const metadata: Metadata = { title: 'Lớp học — AcaWrite' };

export default function Page() {
  return <StudentClassesPage />;
}
