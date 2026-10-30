import type { Metadata } from 'next';

import AdminOverviewPage from './index';

export const metadata: Metadata = { title: 'Tổng quan hệ thống — AcaWrite' };

export default function Page() {
  return <AdminOverviewPage />;
}
