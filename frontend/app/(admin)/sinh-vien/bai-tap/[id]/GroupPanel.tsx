'use client';

import { useCallback, useEffect, useState } from 'react';

import { Alert, App, Button, Card, Input, Skeleton, Space, Tag } from 'antd';
import { LogOut, Plus, UsersRound } from 'lucide-react';

import { paperApi } from '@/lib/api/paper';
import type { StudentGroups } from '@/lib/types/paper';
import { formatDateTime } from '@/lib/utils/date';

// ─── Sinh viên: nhóm viết paper của tôi (UC-34, BR-27) ────────────────────────
// Có nhóm → thành viên + rời nhóm; chưa có → lập nhóm mới hoặc vào nhóm còn chỗ. Sau hạn chốt nhóm chỉ xem.
// Lỗi (đủ người, đã qua hạn, nhóm đã có bài làm…) axiosClient đã toast; trang vẽ lại theo kết quả server.

export function GroupPanel({ assignmentId, onChanged }: { assignmentId: number; onChanged: () => void }) {
  const { modal, message } = App.useApp();
  const [view, setView] = useState<StudentGroups | null>(null);
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    paperApi
      .studentGroups(assignmentId)
      .then((v) => {
        if (!cancelled) setView(v);
      })
      .catch(() => {
        // đã toast
      });
    return () => {
      cancelled = true;
    };
  }, [assignmentId]);

  const run = useCallback(
    async (action: () => Promise<StudentGroups>, done: string) => {
      setBusy(true);
      try {
        setView(await action());
        message.success(done);
        onChanged();
      } catch {
        // đã toast
      } finally {
        setBusy(false);
      }
    },
    [message, onChanged]
  );

  if (!view) {
    return (
      <Card title="Nhóm của tôi">
        <Skeleton active paragraph={{ rows: 3 }} />
      </Card>
    );
  }

  const mine = view.groups.find((g) => g.id === view.myGroupId) ?? null;
  const lockText = view.lockAt ? `Chốt nhóm lúc ${formatDateTime(view.lockAt)}` : 'Chốt nhóm lúc hạn nộp';

  const leave = () =>
    modal.confirm({
      title: `Rời ${mine?.name}?`,
      content: 'Bạn sẽ không xem / sửa được bài của nhóm này nữa. Nhóm đã có bài làm thì không tự rời được.',
      okText: 'Rời nhóm',
      okButtonProps: { danger: true },
      cancelText: 'Ở lại',
      onOk: () => run(() => paperApi.leaveGroup(assignmentId), 'Đã rời nhóm')
    });

  return (
    <Card
      title={
        <span className="inline-flex items-center gap-2">
          <UsersRound size={16} className="text-brand-600" /> Nhóm của tôi
        </span>
      }
    >
      <p className="-mt-1 mb-3 text-caption text-neutral-500">
        Tối đa {view.maxGroupSize} người · {lockText}
      </p>

      {mine ? (
        <div className="flex flex-col gap-3">
          <div>
            <p className="m-0 font-semibold text-neutral-900">{mine.name}</p>
            <ul className="m-0 mt-1 list-none p-0">
              {mine.members.map((m) => (
                <li key={m.studentId} className="text-small text-neutral-700">
                  {m.fullName} <span className="font-mono text-caption text-neutral-500">{m.studentCode}</span>
                </li>
              ))}
            </ul>
          </div>
          {view.canChange && (
            <Button icon={<LogOut size={14} />} onClick={leave} loading={busy} className="self-start" size="small">
              Rời nhóm
            </Button>
          )}
        </div>
      ) : !view.canChange ? (
        <Alert
          type="warning"
          showIcon
          title="Bạn chưa có nhóm"
          description="Đã qua hạn chốt nhóm — hãy liên hệ giáo viên để được xếp nhóm."
        />
      ) : (
        <div className="flex flex-col gap-3">
          <Alert
            type="info"
            showIcon
            title="Bạn chưa có nhóm"
            description="Lập nhóm mới hoặc vào một nhóm còn chỗ để bắt đầu viết."
          />
          <Space.Compact className="w-full">
            <Input
              value={name}
              maxLength={100}
              onChange={(e) => setName(e.target.value)}
              placeholder="Tên nhóm mới"
              onPressEnter={() =>
                name.trim() && run(() => paperApi.createMyGroup(assignmentId, name.trim()), 'Đã lập nhóm')
              }
            />
            <Button
              type="primary"
              icon={<Plus size={14} />}
              disabled={!name.trim()}
              loading={busy}
              onClick={() => run(() => paperApi.createMyGroup(assignmentId, name.trim()), 'Đã lập nhóm')}
            >
              Lập
            </Button>
          </Space.Compact>
          {view.groups.length > 0 && (
            <ul className="m-0 flex list-none flex-col gap-2 p-0">
              {view.groups.map((g) => {
                const full = g.members.length >= view.maxGroupSize;
                return (
                  <li
                    key={g.id}
                    className="flex items-center justify-between gap-2 rounded-md border border-neutral-100 px-3 py-2"
                  >
                    <span className="min-w-0">
                      <span className="font-medium text-neutral-900">{g.name}</span>{' '}
                      <span className="text-caption text-neutral-500 tabular-nums">
                        {g.members.length}/{view.maxGroupSize}
                      </span>
                      <span className="block truncate text-caption text-neutral-500">
                        {g.members.map((m) => m.fullName).join(', ') || 'Chưa có ai'}
                      </span>
                    </span>
                    {full ? (
                      <Tag className="m-0">Đủ người</Tag>
                    ) : (
                      <Button
                        size="small"
                        loading={busy}
                        onClick={() => run(() => paperApi.joinGroup(assignmentId, g.id), `Đã vào ${g.name}`)}
                      >
                        Vào nhóm
                      </Button>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      )}
    </Card>
  );
}
