'use client';

import { useEffect, useMemo, useState } from 'react';

import { Alert, Segmented, Select, Skeleton } from 'antd';

import { studentAnnotationViews } from '@/components/EssayAnnotator';
import { ResolutionList, VersionDiff } from '@/components/VersionCompare';
import type { ResolvedItem } from '@/components/VersionCompare';
import { studentApi } from '@/lib/api/student';
import type { VersionDetail, VersionSummary } from '@/lib/types/student';
import { formatDate } from '@/lib/utils/date';
import { diffWords, guessResolution } from '@/lib/utils/textDiff';

// ─── Sinh viên: so sánh lần nộp đang xem với một lần trước (FR-35, UC-23 A1, Prompt 7 E) ─
// Chỉ xem. Lỗi của lần cũ (khi lần đó đã được trả) hiện đánh giá của giáo viên, chưa đánh giá thì tự đoán.

const groupOfCriterion = (code: string | null) => (code === 'TA' || code === 'TR' ? 'TASK' : code);

export function StudentCompare({
  partId,
  versions,
  current
}: {
  partId: number;
  versions: VersionSummary[];
  current: VersionDetail;
}) {
  const newNo = current.versionNo;
  const older = useMemo(() => versions.filter((v) => v.versionNo < newNo), [versions, newNo]);
  const [oldNo, setOldNo] = useState(older[0]?.versionNo ?? 0);
  const [loaded, setLoaded] = useState<VersionDetail | null>(null);
  const [failed, setFailed] = useState<number | null>(null);
  const [view, setView] = useState<'merged' | 'split'>('merged');
  const [selectedId, setSelectedId] = useState<number | null>(null);

  useEffect(() => {
    if (!oldNo) return;
    let cancelled = false;
    studentApi
      .version(partId, oldNo)
      .then((v) => {
        if (!cancelled) setLoaded(v);
      })
      .catch(() => {
        if (!cancelled) setFailed(oldNo);
      });
    return () => {
      cancelled = true;
    };
  }, [partId, oldNo]);

  const old = loaded && loaded.versionNo === oldNo ? loaded : null;
  const oldText = old?.content ?? '';
  const ops = useMemo(() => (old ? diffWords(oldText, current.content) : []), [old, oldText, current.content]);

  const comments = old?.result?.inlineComments;
  const items = useMemo<ResolvedItem[]>(() => {
    if (!comments) return [];
    const views = studentAnnotationViews(comments);
    return comments
      .map((ic, i) => ({ ic, view: views[i], id: i }))
      .filter(({ ic }) => ic.kind === 'ERROR')
      .sort((a, b) => a.ic.startOffset - b.ic.startOffset)
      .map(({ ic, view: v, id }) => ({
        id,
        view: v,
        guess: guessResolution(oldText, current.content, ops, ic),
        manual: ic.resolution
      }));
  }, [comments, oldText, current.content, ops]);

  const groupOf = useMemo(() => {
    const map = new Map((comments ?? []).map((ic) => [ic.errorCode, groupOfCriterion(ic.criterionCode)]));
    return (code: string) => map.get(code) ?? null;
  }, [comments]);

  if (older.length === 0) return null;
  const selected = items.find((it) => it.id === selectedId);

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <Select
          className="min-w-56"
          value={oldNo}
          onChange={(no: number) => {
            setOldNo(no);
            setFailed(null);
            setSelectedId(null);
          }}
          options={older.map((v) => ({
            value: v.versionNo,
            label: `So sánh lần ${v.versionNo} (${formatDate(v.submittedAt)}) ↔ lần ${newNo}`
          }))}
        />
        <Segmented
          value={view}
          onChange={(v) => setView(v as 'merged' | 'split')}
          options={[
            { value: 'merged', label: 'Gộp' },
            { value: 'split', label: 'Song song' }
          ]}
        />
      </div>

      {failed === oldNo ? (
        <Alert type="error" showIcon title={`Không tải được lần nộp ${oldNo}. Thử chọn lại hoặc tải lại trang.`} />
      ) : !old ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : (
        <>
          <VersionDiff
            ops={ops}
            view={view}
            oldLabel={`Lần ${oldNo} · ${old.wordCount} từ`}
            newLabel={`Lần ${newNo} · ${current.wordCount} từ`}
            mark={selected ? { start: selected.view.startOffset, end: selected.view.endOffset } : null}
          />
          {old.result && (
            <ResolutionList
              items={items}
              groupOf={groupOf}
              oldNo={oldNo}
              selectedId={selectedId}
              onPick={setSelectedId}
            />
          )}
        </>
      )}
    </div>
  );
}
