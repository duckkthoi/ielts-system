'use client';

import { useState } from 'react';

import { Button, Drawer } from 'antd';
import { ListTree } from 'lucide-react';

import type { StudentPartDetail } from '@/lib/types/student';

import { PromptPanel } from './PromptPanel';

// ─── Khung màn làm bài PAPER: một lưới chung cho mục lục + trình soạn ─────────
// Từ lg: cột trái rộng cố định 288px (đề của giai đoạn + mục lục, dính khi cuộn, tự cuộn nếu dài), cột phải là
// trình soạn — hai cột cùng mép trên, cùng container. Nhỏ hơn lg (máy tính bảng, điện thoại): mục lục thu vào drawer,
// mở bằng nút ngay trên trình soạn. Không có thanh cuộn ngang.

function Sidebar({ part, outline }: { part: StudentPartDetail; outline?: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-5">
      <div>
        <h2 className="mb-2 text-h3 font-semibold text-neutral-900">{part.title}</h2>
        <PromptPanel part={part} />
      </div>
      {outline && <div className="border-t border-neutral-100 pt-4">{outline}</div>}
    </div>
  );
}

export function PaperLayout({
  part,
  outline,
  toolbar,
  children
}: {
  part: StudentPartDetail;
  /** Không có (bản nộp cũ chỉ có chữ thuần) → cột trái chỉ có đề. */
  outline?: React.ReactNode;
  toolbar?: React.ReactNode;
  children: React.ReactNode;
}) {
  const [drawerOpen, setDrawerOpen] = useState(false);

  return (
    <div className="mx-auto grid w-full max-w-330 grid-cols-1 gap-6 px-4 py-4 sm:px-6 lg:grid-cols-[288px_minmax(0,1fr)] lg:py-6">
      <aside className="hidden lg:block">
        <div className="sticky top-20 max-h-[calc(100vh-6rem)] overflow-y-auto rounded-lg border border-neutral-100 bg-neutral-0 p-5">
          <Sidebar part={part} outline={outline} />
        </div>
      </aside>
      <section className="flex min-w-0 flex-col gap-4">
        <Button className="self-start lg:hidden" icon={<ListTree size={16} />} onClick={() => setDrawerOpen(true)}>
          Đề bài &amp; mục lục
        </Button>
        {toolbar}
        {children}
      </section>
      <Drawer
        placement="left"
        size={320}
        title="Đề bài & mục lục"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        className="lg:hidden"
      >
        {/* Bấm một mục trong mục lục → đóng drawer để thấy chỗ vừa cuộn tới. */}
        <div onClick={(e) => (e.target as HTMLElement).closest('button') && setDrawerOpen(false)}>
          <Sidebar part={part} outline={outline} />
        </div>
      </Drawer>
    </div>
  );
}
