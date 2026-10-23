import type { Metadata } from 'next';

import UsersPage from './index';

export const metadata: Metadata = { title: 'Người dùng — IELTS Writing' };

export default function Page() {
  return <UsersPage />;
}
