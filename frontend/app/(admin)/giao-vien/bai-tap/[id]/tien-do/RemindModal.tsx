'use client';

import { useMemo, useState } from 'react';

import { App, Modal, Select } from 'antd';

import { statsApi } from '@/lib/api/stats';
import type { ProgressMatrix } from '@/lib/types/stats';
import { remindTargets } from '@/lib/utils/progress';

// ─── Nhắc nhở thủ công (UC-16, FR-52) ─────────────────────────────────────────
// Không chọn ai → nhắc mọi sinh viên còn phần phải nộp (theo phần đang chọn). Sinh viên nhận thông báo
// kèm email; server giới hạn 3 lần / 10 phút cho mỗi bài (429 → toast lỗi của server).

const MAX_NAMES = 8;

export function RemindModal({
  assignmentId,
  matrix,
  selected,
  open,
  onClose
}: {
  assignmentId: number;
  matrix: ProgressMatrix;
  selected: number[];
  open: boolean;
  onClose: (sent: boolean) => void;
}) {
  const { message } = App.useApp();
  const [partId, setPartId] = useState<number | undefined>(undefined);
  const [sending, setSending] = useState(false);

  const targets = useMemo(() => remindTargets(matrix, selected, partId), [matrix, selected, partId]);
  const names = useMemo(() => {
    const byId = new Map(matrix.rows.map((r) => [r.student.id, r.student.fullName]));
    return targets.map((id) => byId.get(id) ?? '');
  }, [matrix, targets]);

  const send = async () => {
    setSending(true);
    try {
      const res = await statsApi.remind(assignmentId, targets, partId);
      message.success(`Đã gửi nhắc nhở cho ${res.sent} sinh viên`);
      onClose(true);
    } catch {
      // axiosClient đã hiện lỗi (vd quá số lần nhắc)
    } finally {
      setSending(false);
    }
  };

  return (
    <Modal
      open={open}
      destroyOnHidden
      title={`Gửi nhắc nhở cho ${targets.length} sinh viên?`}
      okText="Gửi nhắc nhở"
      okButtonProps={{ disabled: targets.length === 0, loading: sending }}
      onOk={send}
      onCancel={() => onClose(false)}
    >
      <div className="flex flex-col gap-3">
        <p className="text-neutral-700">
          {selected.length > 0
            ? 'Gửi tới các sinh viên đang được chọn.'
            : 'Gửi tới các sinh viên còn phần chưa nộp hoặc cần sửa.'}{' '}
          Mỗi người nhận một thông báo và một email nhắc hạn.
        </p>
        <label className="flex flex-col gap-1">
          <span className="text-small text-neutral-500">Nhắc về phần</span>
          <Select<number>
            value={partId ?? 0}
            onChange={(v) => setPartId(v === 0 ? undefined : v)}
            options={[{ value: 0, label: 'Cả bài' }, ...matrix.parts.map((p) => ({ value: p.id, label: p.title }))]}
          />
        </label>
        {targets.length === 0 ? (
          <p className="text-small text-neutral-500">Không còn ai phải nộp phần này.</p>
        ) : (
          <p className="text-small text-neutral-500">
            {names.slice(0, MAX_NAMES).join(', ')}
            {names.length > MAX_NAMES && ` và ${names.length - MAX_NAMES} người khác`}
          </p>
        )}
        <p className="text-caption text-neutral-400">Mỗi bài nhắc tối đa 3 lần trong 10 phút.</p>
      </div>
    </Modal>
  );
}
