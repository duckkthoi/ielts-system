import type { Metadata } from 'next';

import ChangePasswordPage from './index';

export const metadata: Metadata = { title: 'Đổi mật khẩu — AcaWrite' };

export default function Page() {
  return <ChangePasswordPage />;
}
