import { Alert, Collapse, Image } from 'antd';

import { EssayAnnotator } from '@/components/EssayAnnotator';
import type { AnnotateAction, AnnotationView } from '@/components/EssayAnnotator';
import { PaperViewer } from '@/components/PaperEditor';
import { fileUrl } from '@/lib/constants';
import type { GradingScreen } from '@/lib/types/grading';
import { formatDateTime } from '@/lib/utils/date';

// ─── Bài làm bên trái màn chấm: cảnh báo số từ, đề bài (thu gọn được), bài viết ─
// `prompt` là HTML đã được server lọc — trường duy nhất được render bằng dangerouslySetInnerHTML.
// Bài làm vẽ bằng EssayAnnotator: đánh dấu kiểu "Word đã chấm", bôi đen để thêm (chỉ khi còn chấm được).
// Bài PAPER (B25c-2): hiện bản nộp có định dạng bằng PaperViewer (chỉ đọc); nhận xét theo khối làm ở B25d.

export function EssayPanel({
  screen,
  annotations,
  selectedKey,
  scrollNonce,
  onSelect,
  onAction,
  onEdit,
  onDelete
}: {
  screen: GradingScreen;
  annotations: AnnotationView[];
  selectedKey: string | null;
  scrollNonce: number;
  onSelect: (key: string | null) => void;
  onAction: (action: AnnotateAction, range: { start: number; end: number }) => void;
  onEdit: (key: string) => void;
  onDelete: (key: string) => void;
}) {
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
      {version.contentJson ? (
        <PaperViewer
          contentJson={version.contentJson}
          references={version.references ?? []}
          style={screen.assignment.citationStyle ?? 'IEEE'}
          footer={<span className="text-caption text-neutral-500">{version.wordCount} từ</span>}
        />
      ) : (
        <>
          {screen.editable && (
            <p className="text-caption text-neutral-500">
              Bôi đen chữ trong bài để đánh dấu lỗi, tô màu, sửa hoặc nhận xét.
            </p>
          )}
          <EssayAnnotator
            content={version.content}
            annotations={annotations}
            readOnly={!screen.editable}
            selectedKey={selectedKey}
            scrollNonce={scrollNonce}
            onSelect={onSelect}
            onAction={onAction}
            onEdit={onEdit}
            onDelete={onDelete}
          />
        </>
      )}
    </div>
  );
}
