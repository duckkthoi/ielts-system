'use client';

import { useEffect, useState } from 'react';

import { Card, Skeleton } from 'antd';
import { MonitorSmartphone } from 'lucide-react';
import Link from 'next/link';

import { PageHeader } from '@/components/PageHeader';
import { adminApi } from '@/lib/api/admin';

// ─── Quản trị: phiên đăng nhập (UC-08) ────────────────────────────────────────
// API danh sách phiên / buộc đăng xuất từng phiên thuộc Mức 3 (docs/05 A.3), backend chưa có.
// Trang hiện số phiên đang hoạt động và chỉ cách đăng xuất một người: khoá hoặc đặt lại mật khẩu.

export default function SessionsPage() {
  const [activeSessions, setActiveSessions] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    adminApi
      .getStats()
      .then((res) => {
        if (!cancelled) setActiveSessions(res.activeSessions);
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader title="Phiên đăng nhập" description="Các phiên đăng nhập đang hoạt động trên hệ thống." />
      <Card>
        <div className="flex flex-col items-center gap-3 py-8 text-center">
          <span className="grid size-13 place-items-center rounded-[14px] bg-brand-50 text-brand-600">
            <MonitorSmartphone size={24} />
          </span>
          {activeSessions === null ? (
            <Skeleton.Input active />
          ) : (
            <p className="text-h2 font-semibold text-neutral-900">{activeSessions} phiên đang hoạt động</p>
          )}
          <p className="max-w-xl text-neutral-500">
            Danh sách từng phiên (thiết bị, IP) và buộc đăng xuất một phiên sẽ có ở phiên bản sau. Hiện tại, để đăng
            xuất một người khỏi mọi thiết bị, hãy <b>khoá</b> hoặc <b>đặt lại mật khẩu</b> tài khoản đó ở trang{' '}
            <Link href="/quan-tri/nguoi-dung" className="text-brand-600">
              Người dùng
            </Link>
            .
          </p>
        </div>
      </Card>
    </div>
  );
}
