import { Alert, Collapse, Image } from 'antd';

import { fileUrl } from '@/lib/constants';
import type { GradingScreen } from '@/lib/types/grading';
import { formatDateTime } from '@/lib/utils/date';

// ─── Bài làm bên trái màn chấm: cảnh báo số từ, đề bài (thu gọn được), bài viết ─
// `prompt` là HTML đã được server lọc — trường duy nhất được render bằng dangerouslySetInnerHTML.
// Bài làm hiển thị nguyên văn (giữ xuống dòng). Nhận xét inline: bước F5b.

export function EssayPanel({ screen }: { screen: GradingScreen }) {
  const { part, version, warnings } = screen;
  return (
    <div className="flex flex-col gap-3">
      {warnings.map((w) => (
        <Alert key={w.code} type={w.code === 'SHORT_ESSAY' ? 'error' : 'warning'} showIcon title={w.message} />
      ))}
      <Collapse
        className="bg-neutral-0"
        items={[
          {
            key: 'prompt',
            label: (
              <span className="text-small text-neutral-700">
                <strong>Đề bài</strong> · {version.wordCount} từ · nộp lúc {formatDateTime(version.submittedAt)}
              </span>
            ),
            children: (
              <div className="flex flex-col gap-3">
                <div
                  className="font-serif text-prompt text-neutral-900 [&_ol]:list-decimal [&_ol]:pl-5 [&_p]:mb-2 [&_ul]:list-disc [&_ul]:pl-5"
                  dangerouslySetInnerHTML={{ __html: part.prompt }}
                />
                {part.imageUrl && (
                  <Image
                    src={fileUrl(part.imageUrl)}
                    alt={`Hình minh hoạ đề: ${part.title}`}
                    className="max-h-72 rounded-md border border-neutral-100 object-contain"
                  />
                )}
              </div>
            )
          }
        ]}
      />
      <article className="mx-auto w-full max-w-180 rounded-lg border border-neutral-100 bg-neutral-0 px-5 py-6 font-serif text-[16px]/[28px] whitespace-pre-wrap text-neutral-900 shadow-sm sm:px-8 sm:py-8 sm:text-essay">
        {version.content}
      </article>
    </div>
  );
}
