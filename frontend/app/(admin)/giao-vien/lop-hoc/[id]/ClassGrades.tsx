'use client';

import { useEffect, useMemo, useState } from 'react';

import { Button, Card, Input, Skeleton, Table, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { BarChart3 } from 'lucide-react';
import Link from 'next/link';

import { BandBadge } from '@/components/BandBadge';
import { statsApi } from '@/lib/api/stats';
import type { ClassStats, RankingRow } from '@/lib/types/stats';

const bandText = (value: number | null) => (value === null ? '—' : value.toFixed(2));

export function ClassGrades({ classId }: { classId: number }) {
  const [result, setResult] = useState<{ classId: number; stats: ClassStats } | null>(null);
  const [keyword, setKeyword] = useState('');
  const stats = result?.classId === classId ? result.stats : null;

  useEffect(() => {
    let cancelled = false;
    void statsApi
      .classStats(classId)
      .then((data) => {
        if (!cancelled) setResult({ classId, stats: data });
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [classId]);

  const rows = useMemo(() => {
    const value = keyword.trim().toLocaleLowerCase('vi');
    if (!value || !stats) return stats?.ranking ?? [];
    return stats.ranking.filter(
      (row) =>
        row.student.studentCode.toLocaleLowerCase('vi').includes(value) ||
        row.student.fullName.toLocaleLowerCase('vi').includes(value)
    );
  }, [keyword, stats]);

  const columns = useMemo<ColumnsType<RankingRow>>(() => {
    if (!stats) return [];
    return [
      {
        title: 'Sinh viên',
        key: 'student',
        fixed: 'left',
        width: 210,
        render: (_, row) => (
          <div>
            <p className="font-medium text-neutral-900">{row.student.fullName}</p>
            <p className="font-mono text-caption text-neutral-500">{row.student.studentCode}</p>
          </div>
        )
      },
      ...stats.assignments.map((assignment) => ({
        title: (
          <Tooltip title={assignment.title}>
            <Link href={`/giao-vien/bai-tap/${assignment.assignmentId}/thong-ke?lop=${classId}`}>
              <span className="block max-w-36 truncate">{assignment.title}</span>
              {assignment.kind === 'PAPER' && <span className="block text-caption font-normal">Paper · thang 10</span>}
            </Link>
          </Tooltip>
        ),
        key: `assignment-${assignment.assignmentId}`,
        width: 150,
        align: 'center' as const,
        render: (_: unknown, row: RankingRow) => {
          const band = row.assignmentBands[String(assignment.assignmentId)] ?? null;
          return band === null ? (
            <span className="text-neutral-400">—</span>
          ) : (
            <span className="font-semibold tabular-nums text-neutral-900">
              {band.toFixed(2)}
              {assignment.kind === 'PAPER' && <span className="text-caption font-normal text-neutral-500"> / 10</span>}
            </span>
          );
        }
      })),
      {
        title: 'Band trung bình',
        dataIndex: 'averageBand',
        fixed: 'right',
        width: 160,
        render: (band: number | null) => (
          <span className="inline-flex items-center gap-2">
            <BandBadge band={band} size="sm" />
            <span className="font-semibold tabular-nums">{bandText(band)}</span>
          </span>
        )
      },
      {
        title: 'Bài đủ điểm',
        dataIndex: 'gradedAssignments',
        width: 115,
        align: 'center'
      },
      {
        title: 'Phần nộp muộn',
        dataIndex: 'lateParts',
        width: 130,
        align: 'center',
        render: (count: number) => <span className={count > 0 ? 'font-medium text-warning' : ''}>{count}</span>
      }
    ];
  }, [stats, classId]);

  if (!stats) {
    return (
      <Card>
        <Skeleton active paragraph={{ rows: 8 }} />
      </Card>
    );
  }

  return (
    <Card styles={{ body: { padding: 0 } }}>
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-100 p-4">
        <div>
          <h2 className="text-h3 font-semibold text-neutral-900">Bảng điểm lớp</h2>
          <p className="text-small text-neutral-500">
            Mỗi cột là một bài đã giao · dấu — nghĩa là chưa được trả đủ mọi phần · bài Paper chấm thang 10, không tính
            vào band trung bình
          </p>
        </div>
        <div className="flex w-full flex-wrap gap-2 sm:w-auto">
          <Input.Search
            allowClear
            placeholder="Tìm mã SV, họ tên"
            className="w-full sm:w-64"
            onSearch={setKeyword}
            onChange={(event) => !event.target.value && setKeyword('')}
          />
          <Link href={`/giao-vien/thong-ke?lop=${classId}`}>
            <Button icon={<BarChart3 size={16} />}>Xem thống kê đầy đủ</Button>
          </Link>
        </div>
      </div>
      <Table<RankingRow>
        rowKey={(row) => row.student.id}
        columns={columns}
        dataSource={rows}
        pagination={rows.length > 50 ? { pageSize: 50, showSizeChanger: false } : false}
        scroll={{ x: 'max-content' }}
        locale={{ emptyText: keyword ? 'Không có sinh viên nào khớp' : 'Lớp chưa có sinh viên' }}
      />
    </Card>
  );
}
