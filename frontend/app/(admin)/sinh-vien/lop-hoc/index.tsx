'use client';

import { useEffect, useState } from 'react';

import { Button, Card, Empty, Skeleton, Tag } from 'antd';
import { BookOpen } from 'lucide-react';

import { PageHeader } from '@/components/PageHeader';
import { classroomApi } from '@/lib/api/classroom';
import type { StudentClassroomSummary } from '@/lib/types/classroom';

export default function StudentClassesPage() {
  const [classes, setClasses] = useState<StudentClassroomSummary[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    classroomApi
      .listStudentClasses()
      .then((data) => {
        if (!cancelled) setClasses(data);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader title="Lớp học" description="Các lớp bạn đang tham gia và giáo viên phụ trách." />
      {failed ? (
        <Card>
          <Empty description="Không tải được danh sách lớp học.">
            <Button onClick={() => { setFailed(false); setReloadKey((value) => value + 1); }}>Thử lại</Button>
          </Empty>
        </Card>
      ) : !classes ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {[0, 1, 2].map((item) => <Card key={item}><Skeleton active paragraph={{ rows: 3 }} /></Card>)}
        </div>
      ) : classes.length === 0 ? (
        <Card><Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Bạn chưa được thêm vào lớp nào." /></Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {classes.map((item) => (
            <Card key={item.id}>
              <div className="flex items-start gap-3">
                <div className="rounded-lg bg-brand-50 p-2 text-brand-600"><BookOpen size={20} /></div>
                <div className="min-w-0">
                  <h2 className="truncate text-h3 font-semibold text-neutral-900">{item.name}</h2>
                  <p className="font-mono text-small text-neutral-500">{item.code}</p>
                </div>
              </div>
              <p className="mt-4 text-small text-neutral-600">Giáo viên: {item.teacherName}</p>
              {item.description && <p className="mt-2 line-clamp-2 text-small text-neutral-500">{item.description}</p>}
              <div className="mt-4 flex items-center justify-between border-t border-neutral-100 pt-3">
                <Tag color="blue">{item.openAssignmentCount} bài đang mở</Tag>
                <span className="text-caption text-neutral-500">Đang tham gia</span>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
