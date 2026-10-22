import { redirect } from 'next/navigation';

import { ROUTES } from '@/lib/constants';

// Trang đăng nhập tự chuyển về trang chủ theo vai trò nếu đã có phiên.
export default function Home() {
  redirect(ROUTES.login);
}
