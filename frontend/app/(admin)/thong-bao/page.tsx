import type { Metadata } from 'next';

import NotificationsPage from './index';

export const metadata: Metadata = { title: 'Thông báo — IELTS Writing' };

export default function Page() {
  return <NotificationsPage />;
}
