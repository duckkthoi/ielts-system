'use client';

import { useEffect, useState } from 'react';

import { Descriptions, Drawer, Skeleton } from 'antd';

import { AccountStatusTag } from '@/components/AccountStatusTag';
import { adminApi } from '@/lib/api/admin';
import type { UserDetail } from '@/lib/types/admin';
import { formatDateTime } from '@/lib/utils/date';

import { RoleTag } from './const';

// ─── Drawer chi tiết một tài khoản (bấm vào dòng trong bảng) ──────────────────

export function UserDetailDrawer({ userId, onClose }: { userId: number | null; onClose: () => void }) {
  const [detail, setDetail] = useState<UserDetail | null>(null);

  useEffect(() => {
    if (userId === null) return;
    let cancelled = false;
    adminApi
      .getUser(userId)
      .then((res) => {
        if (!cancelled) setDetail(res);
      })
      .catch(onClose);
    return () => {
      cancelled = true;
    };
  }, [userId, onClose]);

  const user = detail?.user.id === userId ? detail.user : null;

  return (
    <Drawer title="Chi tiết tài khoản" size={480} open={userId !== null} onClose={onClose}>
      {!user || !detail ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : (
        <>
          <div className="mb-5">
            <p className="text-h2 font-semibold text-neutral-900">{user.fullName}</p>
            <p className="font-mono text-small text-neutral-500">{user.username}</p>
          </div>
          <Descriptions
            column={1}
            size="small"
            bordered
            items={[
              { key: 'role', label: 'Vai trò', children: <RoleTag role={user.role} /> },
              ...(user.studentCode ? [{ key: 'code', label: 'Mã sinh viên', children: user.studentCode }] : []),
              { key: 'email', label: 'Email', children: user.email ?? '—' },
              {
                key: 'status',
                label: 'Trạng thái',
                children: <AccountStatusTag active={user.active} mustChangePassword={user.mustChangePassword} />
              },
              { key: 'sessions', label: 'Phiên đang hoạt động', children: detail.activeSessions },
              {
                key: 'last',
                label: 'Đăng nhập gần nhất',
                children: user.lastLoginAt ? formatDateTime(user.lastLoginAt) : 'Chưa đăng nhập'
              },
              { key: 'created', label: 'Ngày tạo', children: formatDateTime(user.createdAt) }
            ]}
          />
        </>
      )}
    </Drawer>
  );
}
