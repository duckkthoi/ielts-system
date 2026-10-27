import type { Metadata } from 'next';

import ProfilePage from './index';

export const metadata: Metadata = { title: 'Thông tin cá nhân — IELTS Writing' };

export default function Page() {
  return <ProfilePage />;
}
