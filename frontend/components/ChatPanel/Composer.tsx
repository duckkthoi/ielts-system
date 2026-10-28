'use client';

import { useState } from 'react';

import { Button, Input } from 'antd';
import { Send } from 'lucide-react';

import { MAX_MESSAGE_LENGTH, normalizeMessage } from '@/lib/utils/chat';

// ─── Ô soạn tin: Enter để gửi, Shift + Enter xuống dòng ───────────────────────
// Đang gõ bằng bộ gõ tiếng Việt (IME đang ghép chữ) thì Enter không gửi.

export function Composer({
  connected,
  onSend,
  onTyping
}: {
  connected: boolean;
  onSend: (content: string) => void;
  /** Gọi mỗi lần gõ; ChatPanel tự giới hạn tần suất gửi "đang gõ". */
  onTyping: () => void;
}) {
  const [text, setText] = useState('');
  const content = normalizeMessage(text);
  const tooLong = text.trim().length > MAX_MESSAGE_LENGTH;

  const send = () => {
    if (!content) return;
    onSend(content);
    setText('');
  };

  return (
    <div className="border-t border-neutral-100 p-3">
      {!connected && <p className="mb-2 text-small text-warning">Đang mất kết nối — tin nhắn sẽ chưa gửi được.</p>}
      <div className="flex items-end gap-2">
        <Input.TextArea
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            if (e.target.value.trim()) onTyping();
          }}
          onPressEnter={(e) => {
            if (e.shiftKey || e.nativeEvent.isComposing) return;
            e.preventDefault();
            send();
          }}
          autoSize={{ minRows: 1, maxRows: 5 }}
          placeholder="Nhập tin nhắn… (Enter để gửi, Shift + Enter để xuống dòng)"
          status={tooLong ? 'error' : undefined}
          aria-label="Nội dung tin nhắn"
        />
        <Button type="primary" icon={<Send size={16} />} onClick={send} disabled={!content} aria-label="Gửi" />
      </div>
      {tooLong && <p className="mt-1 text-small text-error">Tin nhắn tối đa {MAX_MESSAGE_LENGTH} ký tự.</p>}
    </div>
  );
}
