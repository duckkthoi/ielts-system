import { Suspense } from 'react';

import type { Metadata } from 'next';

import LoginPage from './index';

export const metadata: Metadata = { title: 'Đăng nhập — AcaWrite' };

export default function Page() {
  return (
    <Suspense>
      <LoginPage />
    </Suspense>
  );
}
