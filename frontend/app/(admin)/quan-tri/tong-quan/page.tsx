import type { Metadata } from 'next';

import AdminOverviewPage from './index';

export const metadata: Metadata = { title: 'Tổng quan hệ thống — IELTS Writing' };

export default function Page() {
  return <AdminOverviewPage />;
}
