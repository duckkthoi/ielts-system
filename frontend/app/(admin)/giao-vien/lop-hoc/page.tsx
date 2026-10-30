import type { Metadata } from 'next';

import ClassListPage from './index';

export const metadata: Metadata = { title: 'Lớp học — AcaWrite' };

export default function Page() {
  return <ClassListPage />;
}
