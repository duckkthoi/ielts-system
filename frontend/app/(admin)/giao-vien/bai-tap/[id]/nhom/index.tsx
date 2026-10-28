'use client';

import { useCallback, useEffect, useState } from 'react';

import { App, Button, Card, Empty, Result, Select, Skeleton, Tag, Tooltip } from 'antd';
import { Pencil, Plus, Shuffle, Trash2, UsersRound } from 'lucide-react';
import { useParams } from 'next/navigation';

import { PageHeader } from '@/components/PageHeader';
import { paperApi } from '@/lib/api/paper';
import type { PaperGroup, TeacherGroups } from '@/lib/types/paper';
import { formatDateTime } from '@/lib/utils/date';

import { AssignmentHeaderFallback, AssignmentSubNav, useAssignmentHeader } from '../AssignmentSubNav';
import { GroupEditModal } from './GroupEditModal';

// ─── Giáo viên: chia nhóm bài Paper (UC-34, BR-27) ────────────────────────────
// Người chưa có nhóm bên trái, các nhóm bên phải. Mọi thao tác trả lại toàn bộ màn hình từ server.
// Giáo viên không bị hạn chốt nhóm; nhóm đã có bài làm thì không xoá được (server báo).

/** Giá trị "tất cả" của ô chọn lớp (antd không hiện nhãn cho `undefined`). */
const ALL = 0;

export default function GroupsPage() {
  const id = Number(useParams<{ id: string }>().id);
  const { modal, message } = App.useApp();
  const { assignment, targets, notFound } = useAssignmentHeader(id);
  const [view, setView] = useState<TeacherGroups | null>(null);
  const [loadError, setLoadError] = useState(false);
  const [editing, setEditing] = useState<PaperGroup | null | undefined>(undefined);
  const [saving, setSaving] = useState(false);
  const [classId, setClassId] = useState<number>(ALL);

  useEffect(() => {
    let cancelled = false;
    paperApi
      .teacherGroups(id)
      .then((v) => {
        if (!cancelled) setView(v);
      })
      .catch(() => {
        if (!cancelled) setLoadError(true);
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  /** Chạy một thao tác rồi vẽ lại theo kết quả server; lỗi axiosClient đã toast. */
  const run = useCallback(
    async (action: () => Promise<TeacherGroups>, done?: string) => {
      setSaving(true);
      try {
        setView(await action());
        if (done) message.success(done);
        return true;
      } catch {
        return false;
      } finally {
        setSaving(false);
      }
    },
    [message]
  );

  if (!assignment || !targets) return <AssignmentHeaderFallback id={id} view="nhom" notFound={notFound} paper />;

  if (assignment.kind !== 'PAPER' || loadError) {
    return (
      <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
        <AssignmentSubNav id={id} view="nhom" />
        <Result status="info" title="Bài này không làm theo nhóm" subTitle="Chỉ bài loại Paper mới có nhóm." />
      </div>
    );
  }

  const save = async (name: string, studentIds: number[]) => {
    const ok = await run(
      () =>
        editing ? paperApi.updateGroup(id, editing.id, name, studentIds) : paperApi.createGroup(id, name, studentIds),
      editing ? 'Đã lưu nhóm' : 'Đã tạo nhóm'
    );
    if (ok) setEditing(undefined);
  };

  const autoSplit = () =>
    modal.confirm({
      title: 'Chia ngẫu nhiên người chưa có nhóm?',
      content: `Tạo các nhóm mới đều nhau, mỗi nhóm tối đa ${view?.maxGroupSize} người${
        classId !== ALL ? ' (chỉ trong lớp đã chọn)' : ''
      }. Nhóm đang có giữ nguyên.`,
      okText: 'Chia nhóm',
      cancelText: 'Huỷ',
      onOk: () => run(() => paperApi.autoGroups(id, classId === ALL ? undefined : classId), 'Đã chia nhóm')
    });

  const remove = (g: PaperGroup) =>
    modal.confirm({
      title: `Xoá ${g.name}?`,
      content: 'Các thành viên trở thành chưa có nhóm. Nhóm đã có bài làm thì không xoá được.',
      okText: 'Xoá',
      okButtonProps: { danger: true },
      cancelText: 'Huỷ',
      onOk: () => run(() => paperApi.deleteGroup(id, g.id), 'Đã xoá nhóm')
    });

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <AssignmentSubNav id={id} view="nhom" paper />
      <PageHeader
        title={`Nhóm — ${assignment.title}`}
        description={
          view ? (
            <span className="inline-flex flex-wrap items-center gap-2">
              Tối đa {view.maxGroupSize} người / nhóm · chốt nhóm{' '}
              {view.lockAt ? formatDateTime(view.lockAt) : 'lúc hạn nộp'}
              {view.locked ? (
                <Tag color="orange">Đã chốt — sinh viên không tự đổi được</Tag>
              ) : (
                <Tag color="green">Sinh viên đang tự lập nhóm được</Tag>
              )}
            </span>
          ) : null
        }
        extra={
          <span className="flex flex-wrap gap-2">
            <Select<number>
              className="w-52"
              value={classId}
              onChange={setClassId}
              options={[
                { value: ALL, label: 'Mọi người nhận bài' },
                ...targets.classes.map((c) => ({ value: c.id, label: `${c.code} – ${c.name}` }))
              ]}
            />
            <Button
              icon={<Shuffle size={16} />}
              onClick={autoSplit}
              disabled={!view?.ungrouped.length}
              loading={saving}
            >
              Chia ngẫu nhiên
            </Button>
            <Button type="primary" icon={<Plus size={16} />} onClick={() => setEditing(null)} disabled={!view}>
              Tạo nhóm
            </Button>
          </span>
        }
      />

      {!view ? (
        <Card>
          <Skeleton active paragraph={{ rows: 6 }} />
        </Card>
      ) : (
        <div className="grid gap-4 lg:grid-cols-[320px_minmax(0,1fr)]">
          <Card title={`Chưa có nhóm (${view.ungrouped.length})`} size="small">
            {view.ungrouped.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Mọi người nhận bài đều đã có nhóm." />
            ) : (
              <ul className="m-0 max-h-[60vh] list-none divide-y divide-neutral-100 overflow-y-auto p-0">
                {view.ungrouped.map((s) => (
                  <li key={s.id} className="py-1.5 text-small">
                    <span className="font-mono text-neutral-500">{s.studentCode}</span> · {s.fullName}
                  </li>
                ))}
              </ul>
            )}
          </Card>

          {view.groups.length === 0 ? (
            <Card>
              <Empty
                image={<UsersRound size={40} className="mx-auto text-neutral-300" />}
                description="Chưa có nhóm nào. Bấm “Chia ngẫu nhiên” hoặc “Tạo nhóm”, hoặc chờ sinh viên tự lập nhóm."
              />
            </Card>
          ) : (
            <div className="grid content-start gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {view.groups.map((g) => (
                <Card
                  key={g.id}
                  size="small"
                  title={
                    <span className="inline-flex items-center gap-2">
                      {g.name}
                      <span className="text-caption font-normal text-neutral-500 tabular-nums">
                        {g.members.length}/{view.maxGroupSize}
                      </span>
                      {g.studentMade && <Tag className="m-0">SV tự lập</Tag>}
                    </span>
                  }
                  extra={
                    <span className="flex">
                      <Tooltip title="Sửa tên / thành viên">
                        <Button
                          type="text"
                          size="small"
                          icon={<Pencil size={14} />}
                          onClick={() => setEditing(g)}
                          aria-label="Sửa nhóm"
                        />
                      </Tooltip>
                      <Tooltip title="Xoá nhóm">
                        <Button
                          type="text"
                          size="small"
                          danger
                          icon={<Trash2 size={14} />}
                          onClick={() => remove(g)}
                          aria-label="Xoá nhóm"
                        />
                      </Tooltip>
                    </span>
                  }
                >
                  {g.members.length === 0 ? (
                    <p className="m-0 text-small text-neutral-500">Chưa có thành viên.</p>
                  ) : (
                    <ul className="m-0 list-none p-0">
                      {g.members.map((m) => (
                        <li key={m.studentId} className="py-0.5 text-small">
                          <span className="font-mono text-neutral-500">{m.studentCode}</span> · {m.fullName}
                        </li>
                      ))}
                    </ul>
                  )}
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {view && (
        <GroupEditModal
          view={view}
          group={editing ?? null}
          open={editing !== undefined}
          saving={saving}
          onClose={() => setEditing(undefined)}
          onSave={save}
        />
      )}
    </div>
  );
}
