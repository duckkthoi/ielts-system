import type { Metadata } from 'next';

import ClassDetailPage from './index';

export const metadata: Metadata = { title: 'Chi tiết lớp — AcaWrite' };

export default function Page() {
  return <ClassDetailPage />;
}
