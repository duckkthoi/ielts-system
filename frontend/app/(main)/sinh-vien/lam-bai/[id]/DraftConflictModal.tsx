import { Button, Modal } from 'antd';

import type { DraftConflict } from '@/lib/types/student';
import { formatTime } from '@/lib/utils/date';
import { countWords } from '@/lib/utils/wordCount';

// ─── Xung đột bản nháp (409 DRAFT_CONFLICT, SD-06) ────────────────────────────
// Tab / máy khác vừa lưu bản mới hơn: cho xem cả hai bản và bắt buộc chọn một (không có nút đóng).

function Preview({ title, text }: { title: string; text: string }) {
  return (
    <div className="flex min-w-0 flex-col gap-2">
      <p className="font-semibold text-neutral-900">{title}</p>
      <div className="h-60 overflow-y-auto rounded-md border border-neutral-100 bg-neutral-25 p-3 font-serif text-small whitespace-pre-wrap text-neutral-700">
        {text || <span className="text-neutral-400">(trống)</span>}
      </div>
      <p className="text-caption text-neutral-500">{countWords(text)} từ</p>
    </div>
  );
}

export function DraftConflictModal({
  conflict,
  localContent,
  onResolve
}: {
  conflict: DraftConflict | null;
  localContent: string;
  onResolve: (choice: 'local' | 'server') => void;
}) {
  return (
    <Modal
      open={conflict !== null}
      title="Bài này vừa được lưu ở một tab hoặc thiết bị khác"
      width={760}
      closable={false}
      mask={{ closable: false }}
      keyboard={false}
      footer={
        <div className="flex flex-wrap justify-end gap-2">
          <Button onClick={() => onResolve('server')}>Dùng bản trên máy chủ</Button>
          <Button type="primary" onClick={() => onResolve('local')}>
            Dùng bản trên máy này
          </Button>
        </div>
      }
    >
      <p className="mb-4 text-neutral-500">Chọn bản muốn giữ. Bản còn lại sẽ bị thay thế.</p>
      <div className="grid gap-4 sm:grid-cols-2">
        <Preview title="Bản trên máy này" text={localContent} />
        <Preview
          title={`Bản trên máy chủ${conflict?.savedAt ? ` (${formatTime(conflict.savedAt)})` : ''}`}
          text={conflict?.serverContent ?? ''}
        />
      </div>
    </Modal>
  );
}
