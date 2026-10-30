import type { Metadata } from 'next';

import GroupsPage from './index';

export const metadata: Metadata = { title: 'Nhóm viết paper — AcaWrite' };

export default function Page() {
  return <GroupsPage />;
}
