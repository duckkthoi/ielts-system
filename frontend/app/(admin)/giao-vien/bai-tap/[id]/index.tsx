'use client';

import { useCallback, useEffect, useState } from 'react';

import { App, Button, Card, Descriptions, Dropdown, Empty, Result, Skeleton, Tabs, Tag } from 'antd';
import { ArrowLeft, BarChart3, ClipboardCheck, MoreHorizontal, Pencil, PieChart, Send, Settings, UserPlus } from 'lucide-react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';

import { editAssignmentPath, useAssignmentActions } from '@/components/AssignmentActions';
import { AssignTargetsModal } from '@/components/AssignTargetsModal';
import { CountdownTimer } from '@/components/CountdownTimer';
import { PageHeader } from '@/components/PageHeader';
import { StatusTag } from '@/components/StatusTag';
import { assignmentApi } from '@/lib/api/assignment';
import { ASSIGNMENT_STATUS_META } from '@/lib/theme/status';
import type { AssignmentDetail, Targets } from '@/lib/types/assignment';
import { formatDateTime } from '@/lib/utils/date';

import { PartView } from './PartView';
import { SettingsDrawer } from './SettingsDrawer';

// ─── Giáo viên: chi tiết một bài tập (UC-12 → UC-14) ──────────────────────────

const LIST_PATH = '/giao-vien/bai-tap';

export default function AssignmentDetailPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const id = Number(useParams<{ id: string }>().id);
  const [assignment, setAssignment] = useState<AssignmentDetail | null>(null);
  const [targets, setTargets] = useState<Targets | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [version, setVersion] = useState(0);
  const [assignMode, setAssignMode] = useState<'publish' | 'add' | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  const { menuItems, run } = useAssignmentActions(reload, () => router.replace(LIST_PATH));

  // Bài của giáo viên khác / không tồn tại → 404.
  useEffect(() => {
    let cancelled = false;
    assignmentApi
      .detail(id)
      .then(async (detail) => {
        const t = detail.status === 'DRAFT' ? null : await assignmentApi.getTargets(id);
        if (cancelled) return;
        setAssignment(detail);
        setTargets(t);
      })
      .catch(() => {
        if (!cancelled) setNotFound(true);
      });
    return () => {
      cancelled = true;
    };
  }, [id, version]);

  const handleAssigned = useCallback(() => {
    setAssignMode(null);
    reload();
  }, [reload]);

  const handleSettingsSaved = useCallback(
    (updated: AssignmentDetail) => {
      setSettingsOpen(false);
      setAssignment(updated);
      message.success('Đã cập nhật bài tập');
    },
    [message]
  );

  const back = (
    <Link
      href={LIST_PATH}
      className="mb-3 inline-flex items-center gap-1 text-small text-neutral-500 hover:text-brand-600"
    >
      <ArrowLeft size={14} /> Bài tập
    </Link>
  );

  if (notFound) {
    return (
      <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
        {back}
        <Result status="404" title="Không tìm thấy bài tập" subTitle="Bài tập không tồn tại hoặc không phải của bạn." />
      </div>
    );
  }

  if (!assignment) {
    return (
      <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
        {back}
        <Skeleton active paragraph={{ rows: 8 }} />
      </div>
    );
  }

  const isDraft = assignment.status === 'DRAFT';
  const actions = (
    <>
      {isDraft ? (
        <>
          <Link href={editAssignmentPath(assignment.id)}>
            <Button icon={<Pencil size={16} />}>Sửa nội dung</Button>
          </Link>
          <Button type="primary" icon={<Send size={16} />} onClick={() => setAssignMode('publish')}>
            Giao bài &amp; phát hành
          </Button>
        </>
      ) : (
        <>
          <Link href={`/giao-vien/bai-tap/${assignment.id}/tien-do`}>
            <Button icon={<BarChart3 size={16} />}>Tiến độ</Button>
          </Link>
          <Link href={`/giao-vien/bai-tap/${assignment.id}/thong-ke`}>
            <Button icon={<PieChart size={16} />}>Thống kê</Button>
          </Link>
          <Link href={`/giao-vien/cham-bai?assignmentId=${assignment.id}`}>
            <Button icon={<ClipboardCheck size={16} />}>Chấm bài</Button>
          </Link>
          {assignment.status === 'PUBLISHED' && (
            <Button icon={<UserPlus size={16} />} onClick={() => setAssignMode('add')}>
              Giao thêm
            </Button>
          )}
        </>
      )}
      <Button icon={<Settings size={16} />} onClick={() => setSettingsOpen(true)}>
        Cài đặt
      </Button>
      <Dropdown
        menu={{ items: menuItems(assignment), onClick: ({ key }) => run(assignment, key) }}
        trigger={['click']}
        placement="bottomRight"
      >
        <Button icon={<MoreHorizontal size={16} />} aria-label="Thao tác khác" />
      </Dropdown>
    </>
  );

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      {back}
      <PageHeader
        title={assignment.title}
        description={
          <span className="inline-flex flex-wrap items-center gap-2">
            <StatusTag meta={ASSIGNMENT_STATUS_META[assignment.status]} size="sm" />
            {assignment.parts.length} phần
            {targets && <> · giao cho {targets.recipientCount} sinh viên</>}
          </span>
        }
        extra={actions}
      />
      {assignment.description && (
        <p className="-mt-2 mb-5 max-w-3xl whitespace-pre-line text-neutral-700">{assignment.description}</p>
      )}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1fr)_340px]">
        <Card title="Các phần" styles={{ body: { paddingTop: 4 } }}>
          {assignment.parts.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Bài chưa có phần nào — hãy sửa nội dung để thêm" />
          ) : (
            <Tabs
              items={assignment.parts.map((part, index) => ({
                key: String(part.id),
                label: `Phần ${index + 1} · ${part.title}`,
                children: <PartView part={part} />
              }))}
            />
          )}
        </Card>

        <div className="flex flex-col gap-4">
          <Card title="Thời gian & tuỳ chọn">
            <Descriptions
              column={1}
              size="small"
              items={[
                {
                  key: 'open',
                  label: 'Mở lúc',
                  children: assignment.openAt ? formatDateTime(assignment.openAt) : 'Ngay khi phát hành'
                },
                {
                  key: 'due',
                  label: 'Hạn nộp',
                  children: assignment.dueAt ? (
                    <span className="flex flex-col items-start gap-1">
                      {formatDateTime(assignment.dueAt)}
                      {assignment.status === 'PUBLISHED' && <CountdownTimer dueAt={assignment.dueAt} />}
                    </span>
                  ) : (
                    <span className="text-warning">Chưa đặt — cần có trước khi phát hành</span>
                  )
                },
                { key: 'late', label: 'Nộp muộn', children: assignment.allowLate ? 'Cho phép' : 'Không cho phép' },
                { key: 'rev', label: 'Yêu cầu sửa', children: `Tối đa ${assignment.maxRevisions} lần` },
                {
                  key: 'sug',
                  label: 'Gợi ý khi sửa',
                  children: assignment.showSuggestionsOnRevision ? 'Hiện' : 'Ẩn'
                },
                ...(assignment.publishedAt
                  ? [{ key: 'pub', label: 'Phát hành lúc', children: formatDateTime(assignment.publishedAt) }]
                  : [])
              ]}
            />
          </Card>

          <Card title="Đã giao cho">
            {!targets ? (
              <p className="text-small text-neutral-500">Bài nháp chưa giao cho ai. Bấm “Giao bài &amp; phát hành”.</p>
            ) : (
              <div className="flex flex-col gap-3">
                {targets.classes.length > 0 && (
                  <div className="flex flex-wrap gap-1.5">
                    {targets.classes.map((c) => (
                      <Link key={c.id} href={`/giao-vien/lop-hoc/${c.id}`}>
                        <Tag className="m-0 cursor-pointer" color="blue">
                          {c.code} · {c.memberCount} SV
                        </Tag>
                      </Link>
                    ))}
                  </div>
                )}
                {targets.students.length > 0 && (
                  <p className="text-small text-neutral-700">
                    Cá nhân: {targets.students.map((s) => `${s.studentCode} (${s.fullName})`).join(', ')}
                  </p>
                )}
                <p className="font-semibold text-neutral-900">
                  Tổng {targets.recipientCount} sinh viên (đã loại trùng)
                </p>
              </div>
            )}
          </Card>
        </div>
      </div>

      {assignMode && (
        <AssignTargetsModal
          assignmentId={assignment.id}
          mode={assignMode}
          open
          dueAt={assignment.dueAt}
          onClose={() => setAssignMode(null)}
          onDone={handleAssigned}
        />
      )}
      <SettingsDrawer
        assignment={assignment}
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        onSaved={handleSettingsSaved}
      />
    </div>
  );
}
