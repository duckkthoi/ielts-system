import { Input } from 'antd';

// ─── Vùng soạn bài (docs/07 mục 7.2 EssayEditor) ──────────────────────────────
// Tờ giấy trắng không viền, font essay (Noto Serif 17/30; điện thoại 16/28), rộng tối đa 720.
// Tắt kiểm tra chính tả của trình duyệt để bài làm phản ánh đúng khả năng như khi thi.

export function EssayEditor({
  value,
  onChange,
  readOnly = false,
  footer
}: {
  value: string;
  onChange?: (text: string) => void;
  readOnly?: boolean;
  footer?: React.ReactNode;
}) {
  return (
    <div className="mx-auto flex w-full max-w-180 flex-col rounded-lg border border-neutral-100 bg-neutral-0 shadow-sm">
      <Input.TextArea
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
        readOnly={readOnly}
        variant="borderless"
        autoSize={{ minRows: 14 }}
        spellCheck={false}
        lang="en"
        placeholder={readOnly ? undefined : 'Bắt đầu viết bài của bạn tại đây…'}
        aria-label="Bài làm"
        className="px-5! py-6! font-serif text-[16px]/[28px]! text-neutral-900 sm:px-8! sm:py-8! sm:text-essay!"
      />
      {footer && (
        <div className="flex flex-wrap items-center justify-between gap-2 border-t border-neutral-100 px-5 py-2 sm:px-8">
          {footer}
        </div>
      )}
    </div>
  );
}
