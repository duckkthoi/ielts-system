'use client';

import { Collapse } from 'antd';

import type { StudentPartDetail } from '@/lib/types/student';

import { PromptPanel } from './PromptPanel';

// ─── Khung một phần: đề bài (trái 40%) + cột làm bài (phải 60%); điện thoại: đề thu gọn ở trên ─
// Khung giãn hết phần còn lại của màn (`flex-1`) để nền cột đề không dừng giữa chừng.
// Bài PAPER: mục lục (`outline`) nằm trên đề bài ở cột trái, điện thoại là một mục thu gọn riêng.

export function Layout({
  part,
  toolbar,
  outline,
  children
}: {
  part: StudentPartDetail;
  toolbar: React.ReactNode;
  outline?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="grid flex-1 grid-cols-1 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
      <aside className="hidden border-r border-neutral-100 bg-neutral-0 lg:block">
        <div className="sticky top-16 max-h-[calc(100vh-4rem)] overflow-y-auto p-6">
          {outline && <div className="mb-6 border-b border-neutral-100 pb-4">{outline}</div>}
          <h2 className="mb-3 text-h2 font-semibold text-neutral-900">{part.title}</h2>
          <PromptPanel part={part} />
        </div>
      </aside>
      <section className="flex min-w-0 flex-col gap-4 px-4 py-4 sm:px-6 lg:py-6">
        <Collapse
          className="bg-neutral-0 lg:hidden"
          items={[
            { key: 'prompt', label: 'Xem đề bài', children: <PromptPanel part={part} /> },
            ...(outline ? [{ key: 'outline', label: 'Mục lục & số từ', children: outline }] : [])
          ]}
        />
        {toolbar}
        {children}
      </section>
    </div>
  );
}
