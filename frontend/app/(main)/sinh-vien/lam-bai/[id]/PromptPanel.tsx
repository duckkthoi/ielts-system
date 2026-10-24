import { Image, Tag } from 'antd';
import { Clock, FileText } from 'lucide-react';

import { fileUrl } from '@/lib/constants';
import type { StudentPartDetail } from '@/lib/types/student';

// ─── Đề bài của phần đang làm ─────────────────────────────────────────────────
// `prompt` là HTML đã được server lọc — trường duy nhất được render bằng dangerouslySetInnerHTML.

export function PromptPanel({ part }: { part: StudentPartDetail }) {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap gap-2">
        {part.minWords ? (
          <Tag className="m-0 inline-flex items-center gap-1">
            <FileText size={12} /> Tối thiểu {part.minWords} từ
          </Tag>
        ) : null}
        {part.suggestedMinutes ? (
          <Tag className="m-0 inline-flex items-center gap-1">
            <Clock size={12} /> Gợi ý {part.suggestedMinutes} phút
          </Tag>
        ) : null}
      </div>
      <div
        className="font-serif text-prompt text-neutral-900 [&_ol]:list-decimal [&_ol]:pl-5 [&_p]:mb-2 [&_ul]:list-disc [&_ul]:pl-5"
        dangerouslySetInnerHTML={{ __html: part.prompt }}
      />
      {part.imageUrl && (
        <Image
          src={fileUrl(part.imageUrl)}
          alt={`Hình minh hoạ đề: ${part.title}`}
          className="rounded-md border border-neutral-100 object-contain"
        />
      )}
    </div>
  );
}
