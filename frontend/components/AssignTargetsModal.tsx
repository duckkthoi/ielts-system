'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { Alert, App, Checkbox, Empty, Modal, Select, Skeleton } from 'antd';

import { assignmentApi } from '@/lib/api/assignment';
import { classroomApi } from '@/lib/api/classroom';
import { getErrorMessage } from '@/lib/apiError';
import type { StudentOption, Targets } from '@/lib/types/assignment';
import type { ClassroomSummary } from '@/lib/types/classroom';
import { formatDateTime } from '@/lib/utils/date';

// ─── Giao bài cho lớp / từng sinh viên ────────────────────────────────────────
// mode "publish": bài nháp → phát hành (UC-14). mode "add": bài đang mở → giao thêm (chỉ thêm, không gỡ).
// Thiếu điều kiện phát hành → server trả NOT_READY liệt kê mọi vấn đề → hiện ngay trong hộp thoại.

const SEARCH_DELAY_MS = 300;

interface AssignTargetsModalProps {
  assignmentId: number;
  mode: 'publish' | 'add';
  open: boolean;
  /** Hạn nộp hiện tại (hiện trong tóm tắt). */
  dueAt?: string | null;
  onClose: () => void;
  onDone: () => void;
}

export function AssignTargetsModal({ assignmentId, mode, open, dueAt, onClose, onDone }: AssignTargetsModalProps) {
  const { message } = App.useApp();
  const [classes, setClasses] = useState<ClassroomSummary[] | null>(null);
  const [current, setCurrent] = useState<Targets | null>(null);
  const [classIds, setClassIds] = useState<number[]>([]);
  const [students, setStudents] = useState<StudentOption[]>([]);
  const [options, setOptions] = useState<StudentOption[]>([]);
  const [searching, setSearching] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Mở hộp thoại → tải lớp của tôi (+ danh sách đã giao nếu giao thêm).
  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    Promise.all([
      classroomApi.listClasses({ page: 0, size: 100 }),
      mode === 'add' ? assignmentApi.getTargets(assignmentId) : Promise.resolve(null)
    ])
      .then(([res, targets]) => {
        if (cancelled) return;
        setClasses(res.data);
        setCurrent(targets);
      })
      .catch(() => {
        if (!cancelled) setClasses([]);
      });
    return () => {
      cancelled = true;
    };
  }, [open, mode, assignmentId]);

  const assignedClassIds = useMemo(() => new Set(current?.classes.map((c) => c.id)), [current]);
  const assignedStudentIds = useMemo(() => new Set(current?.students.map((s) => s.id)), [current]);

  const handleSearch = useCallback((keyword: string) => {
    if (searchTimer.current) clearTimeout(searchTimer.current);
    if (!keyword.trim()) return;
    searchTimer.current = setTimeout(() => {
      setSearching(true);
      assignmentApi
        .searchStudents(keyword.trim())
        .then(setOptions)
        .catch(() => setOptions([]))
        .finally(() => setSearching(false));
    }, SEARCH_DELAY_MS);
  }, []);

  /** Ước tính trước: cộng sĩ số các lớp + số sinh viên chọn riêng (server sẽ loại trùng khi phát hành). */
  const estimated =
    (classes ?? []).filter((c) => classIds.includes(c.id)).reduce((sum, c) => sum + c.memberCount, 0) + students.length;

  const reset = useCallback(() => {
    setClassIds([]);
    setStudents([]);
    setOptions([]);
    setError(null);
  }, []);

  const handleCancel = useCallback(() => {
    reset();
    onClose();
  }, [reset, onClose]);

  const handleOk = useCallback(async () => {
    setSubmitting(true);
    setError(null);
    const body = { classIds, studentIds: students.map((s) => s.id) };
    try {
      if (mode === 'publish') {
        const res = await assignmentApi.publish(assignmentId, body);
        message.success(`${res.message ?? 'Đã phát hành bài tập'} · ${res.data.recipientCount} sinh viên nhận bài`);
      } else {
        const res = await assignmentApi.addTargets(assignmentId, body);
        message.success(`Đã giao thêm cho ${res.data.newRecipients} sinh viên · tổng ${res.data.recipientCount}`);
      }
      reset();
      onDone();
    } catch (e) {
      setError(getErrorMessage(e, 'Giao bài thất bại'));
    } finally {
      setSubmitting(false);
    }
  }, [classIds, students, mode, assignmentId, message, reset, onDone]);

  const nothingChosen = classIds.length === 0 && students.length === 0;

  return (
    <Modal
      title={mode === 'publish' ? 'Giao bài và phát hành' : 'Giao thêm'}
      open={open}
      width={640}
      onCancel={handleCancel}
      onOk={handleOk}
      okText={mode === 'publish' ? 'Phát hành' : 'Giao thêm'}
      cancelText="Huỷ"
      okButtonProps={{ loading: submitting, disabled: mode === 'add' && nothingChosen }}
      destroyOnHidden
    >
      {error && (
        <Alert
          className="mb-4"
          type="error"
          showIcon
          title={mode === 'publish' ? 'Chưa phát hành được' : 'Chưa giao được'}
          description={
            // NOT_READY: "Chưa thể phát hành: vấn đề 1; vấn đề 2" → mỗi vấn đề một dòng.
            <ul className="m-0 list-disc pl-4">
              {error
                .replace(/^Chưa thể phát hành:\s*/, '')
                .split('; ')
                .map((problem) => (
                  <li key={problem}>{problem}</li>
                ))}
            </ul>
          }
        />
      )}
      {mode === 'publish' && (
        <p className="mb-4 text-small text-neutral-500">
          Sinh viên nhận thông báo và email ngay khi phát hành (hoặc khi tới giờ mở nếu bài hẹn giờ). Sau khi phát hành
          chỉ sửa được tên, mô tả, hạn nộp và các tuỳ chọn; không sửa đề và rubric.
        </p>
      )}

      <h3 className="mb-2 font-semibold text-neutral-900">Lớp học</h3>
      {classes === null ? (
        <Skeleton active paragraph={{ rows: 2 }} title={false} />
      ) : classes.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Bạn chưa có lớp nào" />
      ) : (
        <div className="flex max-h-60 flex-col gap-1 overflow-y-auto rounded-md border border-neutral-100 p-2">
          {classes.map((c) => {
            const assigned = assignedClassIds.has(c.id);
            return (
              <Checkbox
                key={c.id}
                className="rounded-md px-2 py-1.5 hover:bg-neutral-25"
                checked={assigned || classIds.includes(c.id)}
                disabled={assigned}
                onChange={(e) =>
                  setClassIds((ids) => (e.target.checked ? [...ids, c.id] : ids.filter((id) => id !== c.id)))
                }
              >
                <span className="font-medium">{c.name}</span>
                <span className="ml-2 font-mono text-small text-neutral-500">{c.code}</span>
                <span className="ml-2 text-small text-neutral-500">
                  · {c.memberCount} sinh viên{assigned ? ' · đã giao' : ''}
                </span>
              </Checkbox>
            );
          })}
        </div>
      )}

      <h3 className="mt-5 mb-2 font-semibold text-neutral-900">Giao cho từng sinh viên</h3>
      <Select<number[]>
        mode="multiple"
        className="w-full"
        placeholder="Gõ mã sinh viên hoặc tên để tìm"
        showSearch={{ filterOption: false, onSearch: handleSearch }}
        loading={searching}
        notFoundContent={searching ? 'Đang tìm…' : 'Gõ để tìm sinh viên'}
        value={students.map((s) => s.id)}
        options={[...students, ...options.filter((o) => !students.some((s) => s.id === o.id))].map((s) => ({
          value: s.id,
          label: `${s.studentCode} · ${s.fullName}`,
          disabled: assignedStudentIds.has(s.id)
        }))}
        onChange={(ids) =>
          setStudents(
            ids
              .map((id) => students.find((s) => s.id === id) ?? options.find((o) => o.id === id))
              .filter((s): s is StudentOption => !!s)
          )
        }
      />

      <div className="mt-5 rounded-md bg-neutral-25 px-4 py-3">
        {mode === 'add' && current && (
          <p className="text-small text-neutral-500">Hiện đang giao cho {current.recipientCount} sinh viên.</p>
        )}
        <p className="font-semibold text-neutral-900">
          {mode === 'add' ? 'Giao thêm' : 'Giao cho'} khoảng {estimated} sinh viên
          <span className="ml-1 text-small font-normal text-neutral-500">(trùng giữa các lớp sẽ được loại)</span>
        </p>
        {dueAt && <p className="text-small text-neutral-500">Hạn nộp {formatDateTime(dueAt)}</p>}
      </div>
    </Modal>
  );
}
