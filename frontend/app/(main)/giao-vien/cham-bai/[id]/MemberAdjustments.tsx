import { Input, Select } from 'antd';

import type { StudentInfo } from '@/lib/types/grading';
import { adjustmentOptions, formatScore, memberScore } from '@/lib/utils/grading';

// ─── Cộng / trừ điểm riêng từng thành viên nhóm (bài PAPER, BR-28, BR-31, docs/12 mục 5.4) ─
// Mặc định cả nhóm cùng điểm. Chọn mức −2 … +2 (bước 0,25) + ghi lý do → điểm người đó = điểm cả bài + mức, kẹp
// [0, 10]. Dòng chưa có lý do chưa được lưu (server bắt buộc lý do) — hiện nhắc ngay dưới ô.

const OPTIONS = [
  { value: 0, label: 'Không cộng / trừ' },
  ...adjustmentOptions().map((d) => ({ value: d, label: `${d > 0 ? '+' : '−'}${formatScore(Math.abs(d))}` }))
];

export function MemberAdjustments({
  members,
  adjustments,
  groupScore,
  disabled,
  onDelta,
  onReason
}: {
  members: StudentInfo[];
  adjustments: Record<number, { delta: number; reason: string }>;
  /** Điểm cả bài đang xem trước (chưa làm tròn cũng được); null khi chưa chấm đủ. */
  groupScore: number | null;
  disabled: boolean;
  onDelta: (studentId: number, delta: number | null) => void;
  onReason: (studentId: number, reason: string) => void;
}) {
  if (members.length === 0) return null;
  return (
    <section className="flex flex-col gap-3 rounded-lg border border-neutral-100 bg-neutral-0 p-4">
      <div>
        <h3 className="font-semibold text-neutral-900">Điểm từng thành viên</h3>
        <p className="text-small text-neutral-500">
          Mặc định cả nhóm cùng điểm. Cộng / trừ tối đa ±2 cho người đóng góp nhiều hoặc ít hơn, bắt buộc ghi lý do.
        </p>
      </div>
      <ul className="m-0 flex list-none flex-col gap-3 p-0">
        {members.map((m) => {
          const adj = adjustments[m.id];
          const delta = adj?.delta ?? 0;
          const score = memberScore(groupScore, delta);
          const missingReason = adj !== undefined && adj.reason.trim() === '';
          return (
            <li key={m.id} className="flex flex-col gap-1">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="min-w-0 truncate">
                  <span className="text-neutral-900">{m.fullName}</span>{' '}
                  <span className="font-mono text-caption text-neutral-500">{m.studentCode}</span>
                </span>
                <span className="inline-flex items-center gap-2">
                  <Select
                    size="small"
                    className="w-40"
                    aria-label={`Cộng / trừ điểm ${m.fullName}`}
                    value={delta}
                    disabled={disabled}
                    options={OPTIONS}
                    onChange={(v: number) => onDelta(m.id, v === 0 ? null : v)}
                  />
                  <span className="w-12 text-right font-semibold text-neutral-900 tabular-nums">
                    {score === null ? '—' : formatScore(score)}
                  </span>
                </span>
              </div>
              {adj && (
                <Input
                  size="small"
                  value={adj.reason}
                  disabled={disabled}
                  maxLength={500}
                  status={missingReason ? 'warning' : undefined}
                  placeholder="Lý do cộng / trừ điểm…"
                  onChange={(e) => onReason(m.id, e.target.value)}
                />
              )}
              {missingReason && !disabled && (
                <p className="text-caption text-warning">Ghi lý do để lưu điểm cộng / trừ của thành viên này.</p>
              )}
            </li>
          );
        })}
      </ul>
    </section>
  );
}
