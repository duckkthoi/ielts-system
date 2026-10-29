import type { Metadata } from 'next';

import ProgressPage from './index';

export const metadata: Metadata = { title: 'Tiến độ bài tập — AcaWrite' };

export default function Page() {
  return <ProgressPage />;
}
