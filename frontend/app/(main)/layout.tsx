'use client';

import { useCallback } from 'react';

import { Button, Layout } from 'antd';
import { ArrowLeft } from 'lucide-react';
import { useRouter } from 'next/navigation';

import { AppHeaderActions } from '@/components/AppHeaderActions';
import { ConnectionBanner } from '@/components/ConnectionBanner';
import { useAuthGuard } from '@/lib/utils/hooks';

// ─── Layout toàn màn hình, chỉ có header ──────────────────────────────────────
// Dùng cho màn hình cần nhiều không gian: làm bài, chấm bài, trao đổi (chat).

export default function FullscreenLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { ready } = useAuthGuard();

  const handleBack = useCallback(() => router.back(), [router]);

  if (!ready) return null;

  return (
    <Layout className="min-h-screen">
      <Layout.Header className="sticky top-0 z-10 flex items-center justify-between border-b border-neutral-100 px-4">
        <Button type="text" icon={<ArrowLeft size={16} />} onClick={handleBack}>
          Quay lại
        </Button>
        <AppHeaderActions />
      </Layout.Header>
      <ConnectionBanner />
      <Layout.Content>{children}</Layout.Content>
    </Layout>
  );
}
