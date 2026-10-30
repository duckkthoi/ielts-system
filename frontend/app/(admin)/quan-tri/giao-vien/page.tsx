import type { Metadata } from 'next';

import TeachersPage from './index';

export const metadata: Metadata = { title: 'Giáo viên — AcaWrite' };

export default function Page() {
  return <TeachersPage />;
}
