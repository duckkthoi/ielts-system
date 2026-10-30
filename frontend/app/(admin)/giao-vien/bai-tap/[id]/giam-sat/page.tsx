import type { Metadata } from 'next';

import MonitorPage from './index';

export const metadata: Metadata = { title: 'Giám sát làm bài — AcaWrite' };

export default function Page() {
  return <MonitorPage />;
}
