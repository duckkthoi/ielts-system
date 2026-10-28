'use client';

import { Collapse } from 'antd';

import type { StudentPartDetail } from '@/lib/types/student';

import { PromptPanel } from './PromptPanel';

// ─── Khung một phần: đề bài (trái 40%) + cột làm bài (phải 60%); điện thoại: đề thu gọn ở trên ─

export function Layout({
  part,
  toolbar,
  children
}: {
  part: StudentPartDetail;
  toolbar: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
      <aside className="hidden border-r border-neutral-100 bg-neutral-0 lg:block">
        <div className="sticky top-16 max-h-[calc(100vh-4rem)] overflow-y-auto p-6">
          <h2 className="mb-3 text-h2 font-semibold text-neutral-900">{part.title}</h2>
          <PromptPanel part={part} />
        </div>
      </aside>
      <section className="flex min-w-0 flex-col gap-4 px-4 py-4 sm:px-6 lg:py-6">
        <Collapse
          className="bg-neutral-0 lg:hidden"
          items={[{ key: 'prompt', label: 'Xem đề bài', children: <PromptPanel part={part} /> }]}
        />
        {toolbar}
        {children}
      </section>
    </div>
  );
}
