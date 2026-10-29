'use client';

import { useCallback, useEffect, useState } from 'react';

import { paperReferenceApi } from '@/lib/api/paperReference';
import type { ReferenceDto } from '@/lib/types/paper';

// ─── Thư viện trích dẫn của nhóm, dùng chung cho drawer thư viện và trình soạn (nhãn trích dẫn, danh mục) ─
// Thành viên khác thêm / sửa mục thì chưa có đẩy realtime (B26): gọi `reload` khi mở drawer / hộp chọn trích dẫn.

export function useGroupReferences(groupId: number | null) {
  const [refs, setRefs] = useState<ReferenceDto[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (groupId === null) return;
    let cancelled = false;
    paperReferenceApi
      .list(groupId)
      .then((list) => {
        if (cancelled) return;
        setRefs(list);
        setFailed(false);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, [groupId, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);

  return { refs, failed, reload };
}
