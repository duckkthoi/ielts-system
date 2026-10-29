import { Suspense } from 'react';

import type { Metadata } from 'next';

import ChatPage from './index';

export const metadata: Metadata = { title: 'Trao đổi — AcaWrite' };

export default function Page() {
  return (
    <Suspense>
      <ChatPage />
    </Suspense>
  );
}
