'use client';

import { useState } from 'react';

import { App, Button, Image, Upload } from 'antd';
import { ImagePlus, Trash2 } from 'lucide-react';

import { fileApi, IMAGE_ACCEPT, IMAGE_MAX_BYTES } from '@/lib/api/file';
import { fileUrl } from '@/lib/constants';

// ─── Ảnh đề (biểu đồ Task 1…): upload ngay khi chọn, form chỉ giữ url `/uploads/…` ─

export function PromptImageInput({
  value,
  onChange
}: {
  value?: string | null;
  onChange?: (url: string | null) => void;
}) {
  const { message } = App.useApp();
  const [uploading, setUploading] = useState(false);

  if (value) {
    return (
      <div className="flex items-start gap-3">
        <Image src={fileUrl(value)} alt="Ảnh đề bài" width={220} className="rounded-md border border-neutral-100" />
        <Button danger icon={<Trash2 size={14} />} onClick={() => onChange?.(null)}>
          Bỏ ảnh
        </Button>
      </div>
    );
  }

  return (
    <Upload.Dragger
      accept={IMAGE_ACCEPT}
      maxCount={1}
      showUploadList={false}
      disabled={uploading}
      beforeUpload={(file) => {
        if (file.size > IMAGE_MAX_BYTES) {
          message.error('Ảnh vượt quá 5 MB');
          return Upload.LIST_IGNORE;
        }
        setUploading(true);
        fileApi
          .uploadImage(file)
          .then((url) => onChange?.(url))
          .catch(() => undefined)
          .finally(() => setUploading(false));
        return false;
      }}
    >
      <div className="flex flex-col items-center gap-1 py-1">
        <ImagePlus size={24} className="text-brand-600" />
        <span className="text-neutral-900">{uploading ? 'Đang tải ảnh lên…' : 'Kéo ảnh vào đây hoặc bấm để chọn'}</span>
        <span className="text-small text-neutral-500">PNG, JPG, WEBP · tối đa 5 MB</span>
      </div>
    </Upload.Dragger>
  );
}
