'use client';

import { useEffect, useMemo, useState } from 'react';

import { Alert, Segmented, Select, Skeleton } from 'antd';

import { ResolutionList, VersionDiff } from '@/components/VersionCompare';
import type { ResolvedItem } from '@/components/VersionCompare';
import { gradingApi } from '@/lib/api/grading';
import type { ErrorCodeDto, GradingScreen, Resolution } from '@/lib/types/grading';
import { formatDate } from '@/lib/utils/date';
import { diffWords, guessResolution } from '@/lib/utils/textDiff';

import { toAnnotationView } from './const';

// ─── So sánh lần nộp đang xem với một lần trước (F5b, docs/08 mục 4.3) ───────
// Nội dung và lỗi của lần cũ lấy từ GET …/{id}?versionNo (không có endpoint riêng); diff theo từ tính ở client.
// Đánh giá tay lưu ngay bằng PATCH …/inline-comments/{commentId} — không đụng tới kết quả chấm đang làm.

export function ComparePanel({ screen, codes }: { screen: GradingScreen; codes: ErrorCodeDto[] }) {
  const submissionId = screen.submission.id;
  const newNo = screen.version.versionNo;
  const older = useMemo(
    () => screen.versions.filter((v) => v.versionNo < newNo).sort((a, b) => b.versionNo - a.versionNo),
    [screen.versions, newNo]
  );
  const [oldNo, setOldNo] = useState(older[0]?.versionNo ?? 0);
  const [loaded, setLoaded] = useState<{ no: number; screen: GradingScreen } | null>(null);
  const [failed, setFailed] = useState<number | null>(null);
  const [view, setView] = useState<'merged' | 'split'>('merged');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);
  // Kết quả PATCH đè lên dữ liệu đã tải (id nhận xét → đánh giá)
  const [manual, setManual] = useState<Record<number, Resolution | null>>({});

  useEffect(() => {
    let cancelled = false;
    gradingApi
      .screen(submissionId, oldNo)
      .then((s) => {
        if (!cancelled) setLoaded({ no: oldNo, screen: s });
      })
      .catch(() => {
        if (!cancelled) setFailed(oldNo);
      });
    return () => {
      cancelled = true;
    };
  }, [submissionId, oldNo]);

  const old = loaded && loaded.no === oldNo ? loaded.screen : null;
  const oldText = old?.version.content ?? '';
  const newText = screen.version.content;
  const ops = useMemo(() => (old ? diffWords(oldText, newText) : []), [old, oldText, newText]);

  const items = useMemo<ResolvedItem[]>(() => {
    if (!old) return [];
    return (old.grade?.inlineComments ?? [])
      .filter((ic) => ic.kind === 'ERROR')
      .sort((a, b) => a.startOffset - b.startOffset)
      .map((ic) => ({
        id: ic.id,
        view: toAnnotationView({ ...ic, key: `s${ic.id}` }, codes, old.criteria),
        guess: guessResolution(oldText, newText, ops, ic),
        manual: ic.id in manual ? manual[ic.id] : ic.resolution
      }));
  }, [old, codes, oldText, newText, ops, manual]);

  const selected = items.find((it) => it.id === selectedId);

  const handleChange = async (id: number, resolution: Resolution | null) => {
    setSavingId(id);
    try {
      const dto = await gradingApi.setResolution(submissionId, id, resolution);
      setManual((m) => ({ ...m, [id]: dto.resolution }));
    } catch {
      // axiosClient đã toast lý do
    } finally {
      setSavingId(null);
    }
  };

  const pickOld = (no: number) => {
    setOldNo(no);
    setFailed(null);
    setSelectedId(null);
    setManual({});
  };

  if (older.length === 0) {
    return <Alert type="info" showIcon title="Đây là lần nộp đầu tiên — chưa có lần nào để so sánh." />;
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <Select
          className="min-w-56"
          value={oldNo}
          onChange={pickOld}
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
        <span className="flex items-center gap-2 text-caption text-neutral-500">
          <span className="rounded-sm bg-diff-removed px-1 text-diff-removed-text line-through">bỏ</span>
          <span className="rounded-sm bg-diff-added px-1 text-diff-added-text">thêm</span>
        </span>
      </div>

      {failed === oldNo ? (
        <Alert type="error" showIcon title={`Không tải được lần nộp ${oldNo}. Thử chọn lại hoặc tải lại trang.`} />
      ) : !old ? (
        <Skeleton active paragraph={{ rows: 8 }} />
      ) : (
        <>
          <VersionDiff
            ops={ops}
            view={view}
            oldLabel={`Lần ${oldNo} · ${old.version.wordCount} từ`}
            newLabel={`Lần ${newNo} · ${screen.version.wordCount} từ`}
            mark={selected ? { start: selected.view.startOffset, end: selected.view.endOffset } : null}
          />
          {old.grade ? (
            <ResolutionList
              items={items}
              groupOf={(code) => codes.find((c) => c.code === code)?.criterion ?? null}
              oldNo={oldNo}
              selectedId={selectedId}
              savingId={savingId}
              onPick={setSelectedId}
              onChange={(id, r) => void handleChange(id, r)}
            />
          ) : (
            <Alert type="info" showIcon title={`Lần ${oldNo} chưa được chấm nên chưa có lỗi để đối chiếu.`} />
          )}
        </>
      )}
    </div>
  );
}
