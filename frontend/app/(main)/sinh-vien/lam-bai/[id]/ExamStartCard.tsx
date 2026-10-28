'use client';

import { useState } from 'react';

import { App, Button } from 'antd';
import { Eye, PlayCircle, ShieldAlert, Timer, TimerOff } from 'lucide-react';

import { examApi } from '@/lib/api/exam';
import type { ExamSession } from '@/lib/types/exam';
import type { StudentPartDetail } from '@/lib/types/student';

// ─── Trước khi thi: luật chế độ thi + nút "Bắt đầu làm bài" (UC-27, BR-23 → BR-26) ─
// Phần ở chế độ thi chưa bắt đầu thì không cho soạn (server cũng trả 409 EXAM_NOT_STARTED).

const RULES = [
  { icon: Timer, text: 'Đồng hồ chạy ngay khi bấm Bắt đầu và không dừng khi tải lại trang hay mở tab khác.' },
  { icon: TimerOff, text: 'Hết giờ bạn vẫn nộp được, nhưng bài sẽ bị đánh dấu "Quá thời gian".' },
  {
    icon: ShieldAlert,
    text: 'Rời khỏi trang làm bài, dán văn bản, mất kết nối đều được ghi lại và báo cho giáo viên.'
  },
  { icon: Eye, text: 'Giáo viên có thể xem trực tiếp bài bạn đang viết và nhắn góp ý ngay trên màn này.' }
];

export function ExamStartCard({
  part,
  onStarted
}: {
  part: StudentPartDetail;
  onStarted: (session: ExamSession) => void;
}) {
  const { modal } = App.useApp();
  const [starting, setStarting] = useState(false);

  const start = async () => {
    setStarting(true);
    try {
      onStarted(await examApi.start(part.partId));
    } catch {
      // axiosClient đã toast lý do (đã nộp, quá hạn, bài đóng…)
    } finally {
      setStarting(false);
    }
  };

  const confirm = () =>
    modal.confirm({
      title: `Bắt đầu ${part.title}?`,
      content: `Bạn có ${part.examDurationMinutes} phút, tính từ bây giờ.`,
      okText: 'Bắt đầu',
      cancelText: 'Để sau',
      onOk: start
    });

  return (
    <div className="mx-auto flex w-full max-w-180 flex-col gap-4 rounded-lg border border-neutral-100 bg-neutral-0 p-6 shadow-sm">
      <div>
        <p className="text-caption font-medium tracking-wide text-brand-600 uppercase">Chế độ thi</p>
        <h3 className="mt-1 text-h3 font-semibold text-neutral-900">
          {part.examDurationMinutes} phút cho {part.title}
        </h3>
      </div>
      <ul className="flex flex-col gap-3">
        {RULES.map(({ icon: Icon, text }) => (
          <li key={text} className="flex gap-3 text-neutral-700">
            <Icon size={18} className="mt-0.5 shrink-0 text-neutral-500" />
            <span>{text}</span>
          </li>
        ))}
      </ul>
      <Button
        type="primary"
        size="large"
        icon={<PlayCircle size={18} />}
        loading={starting}
        onClick={confirm}
        className="self-start"
      >
        Bắt đầu làm bài
      </Button>
    </div>
  );
}
