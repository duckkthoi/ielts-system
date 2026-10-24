'use client';

import { useEffect, useState } from 'react';

import { Badge, Button, Card, Empty, Skeleton, Tabs } from 'antd';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';

import { PageHeader } from '@/components/PageHeader';
import { studentApi } from '@/lib/api/student';
import type { StudentAssignmentList, StudentTab } from '@/lib/types/student';

import { AssignmentCard } from './AssignmentCard';
import { DEFAULT_TAB, isStudentTab, TABS } from './const';

// ─── Sinh viên: bài được giao theo tab (UC-22) ────────────────────────────────
// Tab nằm trên URL (?tab=…) để quay lại từ trang chi tiết vẫn đúng tab. Số trên tab
// server tính trên toàn bộ bài, không phụ thuộc tab đang xem.

export default function StudentAssignmentsPage() {
  const router = useRouter();
  const pathname = usePathname();
  const tabParam = useSearchParams().get('tab');
  const tab: StudentTab = isStudentTab(tabParam) ? tabParam : DEFAULT_TAB;

  const [version, setVersion] = useState(0);
  const [result, setResult] = useState<{ key: string; data: StudentAssignmentList | null } | null>(null);
  const key = `${tab}#${version}`;
  const loading = result?.key !== key;

  useEffect(() => {
    let cancelled = false;
    studentApi
      .assignments(tab)
      .then((data) => {
        if (!cancelled) setResult({ key, data });
      })
      .catch(() => {
        // axiosClient đã toast lỗi; giữ dữ liệu cũ (nếu có) và hiện nút thử lại
        if (!cancelled) setResult((prev) => ({ key, data: prev?.data ?? null }));
      });
    return () => {
      cancelled = true;
    };
  }, [tab, key]);

  const data = result?.data ?? null;
  const current = TABS.find((t) => t.key === tab) ?? TABS[0];

  const changeTab = (next: string) =>
    router.replace(next === DEFAULT_TAB ? pathname : `${pathname}?tab=${next}`, { scroll: false });

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader title="Bài được giao" description="Theo dõi bài cần làm, hạn nộp và kết quả." />

      <Tabs
        activeKey={tab}
        onChange={changeTab}
        items={TABS.map((t) => ({
          key: t.key,
          label: (
            <span className="inline-flex items-center gap-1.5">
              {t.label}
              {data && (
                <Badge
                  count={data.counts[t.key] ?? 0}
                  showZero
                  size="small"
                  color={t.key === 'REVISION' || t.key === 'OVERDUE' ? undefined : 'blue'}
                  className={data.counts[t.key] ? undefined : 'opacity-50'}
                />
              )}
            </span>
          )
        }))}
      />

      {loading && !data ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {[0, 1, 2].map((i) => (
            <Card key={i}>
              <Skeleton active paragraph={{ rows: 4 }} />
            </Card>
          ))}
        </div>
      ) : !data ? (
        <Card>
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa tải được danh sách bài.">
            <Button onClick={() => setVersion((v) => v + 1)}>Thử lại</Button>
          </Empty>
        </Card>
      ) : data.items.length === 0 ? (
        <Card>
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={current.empty} />
        </Card>
      ) : (
        <div className={`grid gap-4 md:grid-cols-2 xl:grid-cols-3 ${loading ? 'opacity-60' : ''}`}>
          {data.items.map((item) => (
            <AssignmentCard key={item.id} item={item} />
          ))}
        </div>
      )}
    </div>
  );
}
