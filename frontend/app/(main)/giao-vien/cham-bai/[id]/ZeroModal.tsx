'use client';

import { useState } from 'react';

import { Modal, Radio } from 'antd';

import { ZERO_REASON_LABEL } from '@/lib/constants';
import type { ZeroReason } from '@/lib/types/grading';

// ─── Chấm 0 cả bài (BR-19, Prompt 6b ý 8): bắt buộc chọn lý do ────────────────

export function ZeroModal({
  open,
  onCancel,
  onConfirm
}: {
  open: boolean;
  onCancel: () => void;
  onConfirm: (reason: ZeroReason) => void;
}) {
  const [reason, setReason] = useState<ZeroReason>();
  return (
    <Modal
      open={open}
      title="Chấm 0 cả bài"
      okText="Xác nhận chấm 0"
      cancelText="Huỷ"
      okButtonProps={{ danger: true, disabled: !reason }}
      onOk={() => reason && onConfirm(reason)}
      onCancel={onCancel}
      destroyOnHidden
    >
      <p className="mb-3 text-neutral-500">Mọi tiêu chí sẽ được đặt về 0. Chọn lý do:</p>
      <Radio.Group
        value={reason}
        onChange={(e) => setReason(e.target.value as ZeroReason)}
        className="flex flex-col gap-2"
        options={(Object.keys(ZERO_REASON_LABEL) as ZeroReason[]).map((r) => ({
          value: r,
          label: ZERO_REASON_LABEL[r]
        }))}
      />
    </Modal>
  );
}
