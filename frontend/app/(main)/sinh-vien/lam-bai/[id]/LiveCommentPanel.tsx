'use client';

import { Collapse } from 'antd';
import { MessageSquareText } from 'lucide-react';

import { ChatPanel } from '@/components/ChatPanel';
import { useAuthStore } from '@/stores/useAuthStore';

// ─── Bình luận trực tiếp của giáo viên ngay trên màn làm bài (BR-26) ──────────
// Là kênh chat `submission-{id}` (đã có), nên tin được lưu, có "đã xem" và sinh viên trả lời được.
// Chưa lưu nháp lần nào thì chưa có bài làm trên server → chưa có kênh.

export function LiveCommentPanel({ submissionId }: { submissionId: number | null }) {
  const user = useAuthStore((s) => s.user);
  if (!user) return null;
  return (
    <Collapse
      className="mx-auto w-full max-w-180 bg-neutral-0"
      defaultActiveKey={['comments']}
      items={[
        {
          key: 'comments',
          label: (
            <span className="inline-flex items-center gap-2 font-medium text-neutral-900">
              <MessageSquareText size={16} className="text-brand-600" />
              Trao đổi với giáo viên
            </span>
          ),
          styles: { body: { padding: 0 } },
          children:
            submissionId === null ? (
              <p className="px-4 py-3 text-small text-neutral-500">
                Góp ý của giáo viên sẽ hiện ở đây sau khi bài được tự lưu lần đầu.
              </p>
            ) : (
              <div className="h-80">
                <ChatPanel
                  key={submissionId}
                  channelId={`submission-${submissionId}`}
                  title="Góp ý trực tiếp"
                  subtitle="Giáo viên có thể xem bài bạn đang viết và nhắn ở đây"
                  myId={user.id}
                  myName={user.fullName}
                  isTeacher={false}
                />
              </div>
            )
        }
      ]}
    />
  );
}
