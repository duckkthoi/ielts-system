import type { Metadata } from 'next';

import AuditLogPage from './index';

export const metadata: Metadata = { title: 'Nhật ký hệ thống — IELTS Writing' };

export default function Page() {
  return <AuditLogPage />;
}
