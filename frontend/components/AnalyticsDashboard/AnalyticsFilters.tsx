'use client';

import { useEffect, useState } from 'react';

import { Button, DatePicker, Select, Tag } from 'antd';
import dayjs from 'dayjs';
import { X } from 'lucide-react';

import { adminApi } from '@/lib/api/admin';
import { assignmentApi } from '@/lib/api/assignment';
import { classroomApi } from '@/lib/api/classroom';
import type { AnalyticsFilter, AnalyticsScope } from '@/lib/types/analytics';

// ─── Bộ lọc thống kê tổng hợp: một hàng phía trên biểu đồ + chip đang lọc ─────
// Giáo viên: lớp + bài của mình. Admin: theo giáo viên (không chọn lớp / bài của người khác ở đây).

type Option = { value: number; label: string };

const KIND_OPTIONS = [
  { value: 'ESSAY', label: 'Bài luận IELTS' },
  { value: 'PAPER', label: 'Paper theo nhóm' }
];
const STATUS_OPTIONS = [
  { value: 'PUBLISHED', label: 'Đang mở' },
  { value: 'CLOSED', label: 'Đã đóng' }
];

export function AnalyticsFilters({
  scope,
  value,
  onChange
}: {
  scope: AnalyticsScope;
  value: AnalyticsFilter;
  onChange: (next: AnalyticsFilter) => void;
}) {
  const [classes, setClasses] = useState<Option[]>([]);
  const [assignments, setAssignments] = useState<Option[]>([]);
  const [teachers, setTeachers] = useState<Option[]>([]);

  useEffect(() => {
    let cancelled = false;
    if (scope === 'teacher') {
      classroomApi
        .listClasses({ page: 0, size: 200 })
        .then((res) => !cancelled && setClasses(res.data.map((c) => ({ value: c.id, label: `${c.code} – ${c.name}` }))))
        .catch(() => undefined);
      assignmentApi
        .list({ page: 0, size: 200 })
        .then(
          (res) =>
            !cancelled &&
            setAssignments(res.data.filter((a) => a.status !== 'DRAFT').map((a) => ({ value: a.id, label: a.title })))
        )
        .catch(() => undefined);
    } else {
      adminApi
        .listTeachers({ page: 0, size: 200 })
        .then((res) => !cancelled && setTeachers(res.data.map((t) => ({ value: t.id, label: t.fullName }))))
        .catch(() => undefined);
    }
    return () => {
      cancelled = true;
    };
  }, [scope]);

  const set = (patch: Partial<AnalyticsFilter>) => onChange({ ...value, ...patch });
  const labelOf = (opts: { value: number | string; label: string }[], v: number | string | undefined) =>
    opts.find((o) => o.value === v)?.label;

  const chips: { key: keyof AnalyticsFilter | 'range'; label: string }[] = [
    ...(value.from || value.to
      ? [{ key: 'range' as const, label: `${value.from ?? '…'} → ${value.to ?? 'hôm nay'}` }]
      : []),
    ...(value.teacherId
      ? [{ key: 'teacherId' as const, label: `GV: ${labelOf(teachers, value.teacherId) ?? ''}` }]
      : []),
    ...(value.classId ? [{ key: 'classId' as const, label: `Lớp: ${labelOf(classes, value.classId) ?? ''}` }] : []),
    ...(value.assignmentId
      ? [{ key: 'assignmentId' as const, label: `Bài: ${labelOf(assignments, value.assignmentId) ?? ''}` }]
      : []),
    ...(value.kind ? [{ key: 'kind' as const, label: labelOf(KIND_OPTIONS, value.kind) ?? '' }] : []),
    ...(value.status ? [{ key: 'status' as const, label: labelOf(STATUS_OPTIONS, value.status) ?? '' }] : [])
  ];

  const remove = (key: keyof AnalyticsFilter | 'range') =>
    key === 'range' ? set({ from: undefined, to: undefined }) : set({ [key]: undefined });

  return (
    <div className="mb-4 flex flex-col gap-2">
      <div className="flex flex-wrap gap-2">
        <DatePicker.RangePicker
          format="DD/MM/YYYY"
          placeholder={['Từ ngày', 'Đến ngày']}
          value={[value.from ? dayjs(value.from) : null, value.to ? dayjs(value.to) : null]}
          onChange={(range) => set({ from: range?.[0]?.format('YYYY-MM-DD'), to: range?.[1]?.format('YYYY-MM-DD') })}
        />
        {scope === 'admin' ? (
          <Select<number>
            allowClear
            showSearch={{ optionFilterProp: 'label' }}
            placeholder="Giáo viên"
            className="w-52"
            value={value.teacherId}
            options={teachers}
            onChange={(teacherId) => set({ teacherId })}
          />
        ) : (
          <>
            <Select<number>
              allowClear
              showSearch={{ optionFilterProp: 'label' }}
              placeholder="Lớp"
              className="w-52"
              value={value.classId}
              options={classes}
              onChange={(classId) => set({ classId })}
            />
            <Select<number>
              allowClear
              showSearch={{ optionFilterProp: 'label' }}
              placeholder="Bài tập"
              className="w-56"
              value={value.assignmentId}
              options={assignments}
              onChange={(assignmentId) => set({ assignmentId })}
            />
          </>
        )}
        <Select
          allowClear
          placeholder="Loại bài"
          className="w-40"
          value={value.kind}
          options={KIND_OPTIONS}
          onChange={(kind?: AnalyticsFilter['kind']) => set({ kind })}
        />
        <Select
          allowClear
          placeholder="Trạng thái bài"
          className="w-36"
          value={value.status}
          options={STATUS_OPTIONS}
          onChange={(status?: AnalyticsFilter['status']) => set({ status })}
        />
      </div>
      {chips.length > 0 && (
        <div className="flex flex-wrap items-center gap-1.5">
          {chips.map((c) => (
            <Tag key={c.key} closable onClose={() => remove(c.key)} className="m-0">
              {c.label}
            </Tag>
          ))}
          <Button size="small" type="link" icon={<X size={12} />} onClick={() => onChange({})}>
            Xoá bộ lọc
          </Button>
        </div>
      )}
    </div>
  );
}
