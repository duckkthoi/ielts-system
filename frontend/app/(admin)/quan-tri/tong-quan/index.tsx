'use client';

import { AnalyticsDashboard } from '@/components/AnalyticsDashboard';
import { PageHeader } from '@/components/PageHeader';

// ─── Quản trị: tổng quan hệ thống (UC-09, H5) ─────────────────────────────────
// Thống kê tổng hợp toàn hệ thống: người dùng, phiên, lớp, bài, tiến độ nộp, điểm, hoạt động; lọc theo kỳ,
// giáo viên, loại bài, trạng thái; xuất bảng sinh viên CSV / Excel.

export default function AdminOverviewPage() {
  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader
        title="Tổng quan hệ thống"
        description="Người dùng, lớp học, bài tập, bài nộp và kết quả — chọn kỳ và bộ lọc để so sánh với kỳ trước."
      />
      <AnalyticsDashboard scope="admin" />
    </div>
  );
}
