import type { Metadata } from 'next';

import SessionsPage from './index';

export const metadata: Metadata = { title: 'Phiên đăng nhập — IELTS Writing' };

export default function Page() {
  return <SessionsPage />;
}
