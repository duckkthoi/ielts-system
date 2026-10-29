import type { Metadata } from 'next';

import AuditLogPage from './index';

export const metadata: Metadata = { title: 'Nhật ký hệ thống — AcaWrite' };

export default function Page() {
  return <AuditLogPage />;
}
