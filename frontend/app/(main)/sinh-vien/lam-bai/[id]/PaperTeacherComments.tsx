'use client';

import { useCallback } from 'react';

import { App } from 'antd';
import { MessageSquareText } from 'lucide-react';

import { LiveCommentList, useCommentSelection, useLiveComments } from '@/components/PaperLiveComments';
import { paperLiveCommentApi } from '@/lib/api/paperLiveComment';
import type { SectionDto } from '@/lib/types/assignment';
import type { LiveComment, LiveCommentPush } from '@/lib/types/paper';
import { openCount } from '@/lib/utils/liveComments';

// ─── Sinh viên: nhận xét trực tiếp của giáo viên trên tài liệu nhóm (H4) ──────
// Đoạn được nhận xét tô vàng trên trình soạn; danh sách nằm dưới trình soạn. Nhận xét mới chỉ báo bằng thông báo nhỏ
// (không lấy focus khỏi trình soạn). Thành viên nhóm đánh dấu "đã xử lý" được; không sửa / xoá được nhận xét.

export function usePaperTeacherComments(documentId: number | null, sections: SectionDto[]) {
  const { message } = App.useApp();
  const onPush = useCallback(
    (push: LiveCommentPush) => {
      if (push.type === 'CREATED')
        message.info({ content: 'Giáo viên vừa thêm nhận xét vào bài', key: 'live-comment' });
    },
    [message]
  );
  const live = useLiveComments(documentId, onPush);
  const selection = useCommentSelection(live.comments);

  const toggleResolved = async (c: LiveComment) => {
    if (documentId === null) return;
    try {
      const res = await paperLiveCommentApi.resolve(documentId, c.id, !c.resolvedAt);
      live.upsert(res.data);
    } catch {
      // axiosClient đã báo lỗi
    }
  };

  const panel =
    documentId === null ? null : (
      <section className="rounded-lg border border-neutral-100 bg-neutral-0 p-4">
        <h3 className="mb-3 flex items-center gap-2 text-h3 font-semibold text-neutral-900">
          <MessageSquareText size={16} className="text-brand-600" />
          Nhận xét của giáo viên
          {openCount(live.comments) > 0 && (
            <span className="text-small font-normal text-neutral-500">· {openCount(live.comments)} chưa xử lý</span>
          )}
        </h3>
        <LiveCommentList
          comments={live.comments}
          staleIds={selection.staleIds}
          activeId={selection.activeId}
          loading={live.loading}
          error={live.error}
          onRetry={live.retry}
          onSelect={selection.selectFromList}
          sectionTitle={(key) => sections.find((s) => s.sectionKey === key)?.title}
          canManage={() => false}
          onToggleResolved={toggleResolved}
          emptyText="Chưa có nhận xét nào. Giáo viên có thể góp ý trực tiếp khi nhóm đang viết."
        />
      </section>
    );

  return { layer: selection.layer, panel };
}
