'use client';

import { useCallback, useMemo, useState } from 'react';

import { App, Button, Card, DatePicker, Input, Result, Select, Table } from 'antd';
import type { TableProps } from 'antd';
import type { Dayjs } from 'dayjs';
import { Eraser, RefreshCw } from 'lucide-react';

import { PageHeader } from '@/components/PageHeader';
import { adminApi } from '@/lib/api/admin';
import type { Role } from '@/lib/constants';
import type { SessionListParams, SessionRow, SessionStatus } from '@/lib/types/admin';
import { useServerList } from '@/lib/utils/hooks';

import { buildColumns, PAGE_SIZE, ROLE_OPTIONS, SORT_FIELDS, STATUS_OPTIONS } from './const';

// ─── Quản trị: phiên đăng nhập (UC-08) ────────────────────────────────────────
// Bảng phiên có lọc tên / vai trò / trạng thái / ngày đăng nhập, phân trang + sắp xếp phía server. Thu hồi một phiên
// hoặc mọi phiên của một tài khoản: thiết bị đó nhận FORCE_LOGOUT ngay (WebSocket), REST / refresh bị từ chối.
// Không có token trong dữ liệu; IP đã được server che một phần.

export default function SessionsPage() {
  const { modal, message } = App.useApp();
  const [query, setQuery] = useState<SessionListParams>({ page: 0, size: PAGE_SIZE, status: 'ACTIVE' });
  const { data, total, loading, error, reload } = useServerList(adminApi.listSessions, query);
  // Dòng vừa thu hồi: cập nhật tại chỗ, không tải lại cả bảng.
  const [patched, setPatched] = useState<Record<number, SessionRow>>({});
  const rows = useMemo(() => data.map((r) => patched[r.sessionId] ?? r), [data, patched]);

  const update = (patch: Partial<SessionListParams>) => setQuery((q) => ({ ...q, ...patch, page: 0 }));

  const handleRange = (range: [Dayjs | null, Dayjs | null] | null) =>
    update({
      from: range?.[0]?.startOf('day').toISOString(),
      to: range?.[1]?.add(1, 'day').startOf('day').toISOString()
    });

  const revokeOne = useCallback(
    (row: SessionRow) =>
      modal.confirm({
        title: `Thu hồi phiên của ${row.fullName}?`,
        content: `Thiết bị ${row.device} (${row.ipMasked ?? 'không rõ IP'}) bị đăng xuất ngay; người dùng phải đăng nhập lại.`,
        okText: 'Thu hồi',
        okButtonProps: { danger: true },
        cancelText: 'Huỷ',
        onOk: async () => {
          const res = await adminApi.revokeSession(row.sessionId);
          setPatched((p) => ({ ...p, [row.sessionId]: res.data }));
          message.success(res.message ?? 'Đã thu hồi phiên đăng nhập');
        }
      }),
    [modal, message]
  );

  const revokeAll = useCallback(
    (row: SessionRow) =>
      modal.confirm({
        title: `Thu hồi mọi phiên của ${row.fullName}?`,
        content: row.current
          ? 'Các thiết bị khác của bạn bị đăng xuất; phiên đang dùng được giữ lại.'
          : 'Mọi thiết bị đang đăng nhập bằng tài khoản này bị đăng xuất ngay.',
        okText: 'Thu hồi tất cả',
        okButtonProps: { danger: true },
        cancelText: 'Huỷ',
        onOk: async () => {
          const res = await adminApi.revokeUserSessions(row.userId);
          message.success(res.message ?? 'Đã thu hồi phiên');
          setPatched({});
          reload();
        }
      }),
    [modal, message, reload]
  );

  const cleanup = () =>
    modal.confirm({
      title: 'Dọn phiên đã hết hạn?',
      content:
        'Xoá khỏi hệ thống các phiên đã quá hạn (người dùng đã phải đăng nhập lại). Không ảnh hưởng phiên đang hoạt động.',
      okText: 'Dọn',
      cancelText: 'Huỷ',
      onOk: async () => {
        const res = await adminApi.cleanupSessions();
        message.success(res.message ?? 'Đã dọn phiên hết hạn');
        setPatched({});
        reload();
      }
    });

  const columns = useMemo(() => buildColumns(revokeOne, revokeAll), [revokeOne, revokeAll]);

  const handleTableChange: TableProps<SessionRow>['onChange'] = (pagination, _filters, sorter) => {
    const s = Array.isArray(sorter) ? sorter[0] : sorter;
    const field = typeof s?.columnKey === 'string' && SORT_FIELDS.has(s.columnKey) && s.order ? s.columnKey : undefined;
    setQuery((q) => ({
      ...q,
      page: (pagination.current ?? 1) - 1,
      sort: field ? `${field},${s?.order === 'ascend' ? 'asc' : 'desc'}` : undefined
    }));
  };

  return (
    <div className="mx-auto w-full max-w-360 px-4 py-6 sm:px-6">
      <PageHeader
        title="Phiên đăng nhập"
        description="Thiết bị đang đăng nhập vào hệ thống. Thu hồi một phiên để buộc thiết bị đó đăng xuất ngay."
        extra={
          <>
            <Button icon={<RefreshCw size={16} />} onClick={() => reload()} loading={loading}>
              Làm mới
            </Button>
            <Button icon={<Eraser size={16} />} onClick={cleanup}>
              Dọn phiên hết hạn
            </Button>
          </>
        }
      />
      <Card styles={{ body: { padding: 0 } }}>
        <div className="flex flex-wrap gap-2 border-b border-neutral-100 p-4">
          <Input.Search
            allowClear
            placeholder="Tên đăng nhập, họ tên"
            className="w-full sm:w-60"
            onSearch={(keyword) => update({ keyword: keyword.trim() || undefined })}
          />
          <Select
            allowClear
            placeholder="Vai trò"
            className="w-36"
            options={ROLE_OPTIONS}
            onChange={(role?: Role) => update({ role })}
          />
          <Select
            allowClear
            placeholder="Trạng thái"
            className="w-44"
            value={query.status}
            options={STATUS_OPTIONS}
            onChange={(status?: SessionStatus) => update({ status })}
          />
          <DatePicker.RangePicker
            format="DD/MM/YYYY"
            placeholder={['Đăng nhập từ', 'Đến ngày']}
            onChange={(range) => handleRange(range)}
          />
        </div>
        {error && rows.length === 0 ? (
          <Result
            status="warning"
            title="Không tải được danh sách phiên"
            extra={
              <Button type="primary" onClick={() => reload()}>
                Thử lại
              </Button>
            }
          />
        ) : (
          <Table<SessionRow>
            rowKey="sessionId"
            size="middle"
            columns={columns}
            dataSource={rows}
            loading={loading}
            scroll={{ x: 560 }}
            onChange={handleTableChange}
            locale={{ emptyText: 'Không có phiên nào khớp bộ lọc' }}
            pagination={{
              current: (query.page ?? 0) + 1,
              pageSize: PAGE_SIZE,
              total,
              showSizeChanger: false,
              showTotal: (n) => `${n} phiên`
            }}
          />
        )}
      </Card>
    </div>
  );
}
