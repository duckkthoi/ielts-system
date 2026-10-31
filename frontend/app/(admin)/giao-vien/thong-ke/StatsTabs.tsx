'use client';

import { Segmented } from 'antd';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';

import { AnalyticsDashboard } from '@/components/AnalyticsDashboard';
import { PageHeader } from '@/components/PageHeader';

import ClassStatsPage from './index';

// ─── Giáo viên: Thống kê = Tổng quan (H5, mọi lớp / bài của mình, có bộ lọc) · Theo lớp (xếp hạng, F6d) ─
// Tab nằm trên URL: `?xem=lop` hoặc có `?lop=` (link cũ từ trang lớp) → tab Theo lớp.

type Tab = 'tong-quan' | 'lop';

export function StatsTabs() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const tab: Tab = params.get('xem') === 'lop' || params.has('lop') ? 'lop' : 'tong-quan';

  const tabs = (
    <Segmented<Tab>
      value={tab}
      onChange={(next) => router.replace(next === 'lop' ? `${pathname}?xem=lop` : pathname, { scroll: false })}
      options={[
        { value: 'tong-quan', label: 'Tổng quan' },
        { value: 'lop', label: 'Theo lớp' }
      ]}
    />
  );

  if (tab === 'lop') {
    return (
      <>
        <div className="mx-auto w-full max-w-360 px-4 pt-6 sm:px-6">{tabs}</div>
        <ClassStatsPage />
      </>
    );
  }
  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <div className="mb-4">{tabs}</div>
      <PageHeader
        title="Thống kê tổng quan"
        description="Các lớp và bài của bạn: tiến độ nộp, điểm, lỗi hay gặp, hoạt động và sinh viên cần chú ý."
      />
      <AnalyticsDashboard scope="teacher" />
    </div>
  );
}
