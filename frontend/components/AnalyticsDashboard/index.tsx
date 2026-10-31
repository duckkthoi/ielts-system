'use client';

import { useEffect, useState } from 'react';

import { Button, Card, Empty, Result, Skeleton } from 'antd';
import { RefreshCw } from 'lucide-react';

import { BandDistributionChart } from '@/components/BandDistributionChart';
import { CriteriaBars } from '@/components/CriteriaBars';
import { analyticsApi } from '@/lib/api/analytics';
import { ROLE_LABEL, type Role } from '@/lib/constants';
import type { AnalyticsFilter, AnalyticsOverview, AnalyticsScope, ScoreSummary } from '@/lib/types/analytics';
import { delta } from '@/lib/utils/analytics';
import { formatDate, formatTime } from '@/lib/utils/date';

import { ActivityHeatmap } from './ActivityHeatmap';
import { AnalyticsFilters } from './AnalyticsFilters';
import { DailyLineChart } from './DailyLineChart';
import { KpiRow } from './KpiRow';
import { StatusStackChart } from './StatusStackChart';
import { StudentAlertsTable } from './StudentAlertsTable';

// ─── Thống kê tổng hợp có bộ lọc (H5): admin toàn hệ thống, giáo viên lớp / bài của mình ─
// Số liệu do server tổng hợp (không tải bài nộp về). Thiếu dữ liệu thì hiện ô trống có lời giải thích, không vẽ biểu đồ giả.

const KIND_LABEL: Record<string, string> = { ESSAY: 'Bài luận', PAPER: 'Paper' };
const STATUS_LABEL: Record<string, string> = { DRAFT: 'nháp', PUBLISHED: 'đang mở', CLOSED: 'đã đóng' };

function Tile({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-neutral-100 bg-neutral-0 p-4">
      <p className="mb-1 text-caption font-medium text-neutral-500">{label}</p>
      <div className="text-small text-neutral-700">{children}</div>
    </div>
  );
}

function Summary({ s, scale }: { s: ScoreSummary; scale: string }) {
  if (s.count === 0) return null;
  return (
    <p className="mb-2 text-small text-neutral-500 tabular-nums">
      {s.count} kết quả · TB {s.mean} · trung vị {s.median} · thấp nhất {s.min} · cao nhất {s.max} {scale}
    </p>
  );
}

const NoData = ({ text }: { text: string }) => <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={text} />;

export function AnalyticsDashboard({ scope }: { scope: AnalyticsScope }) {
  const [filter, setFilter] = useState<AnalyticsFilter>({});
  const [version, setVersion] = useState(0);
  const [state, setState] = useState<{ key: string; data: AnalyticsOverview | null; failed: boolean } | null>(null);
  const key = `${JSON.stringify(filter)}#${version}`;

  useEffect(() => {
    let cancelled = false;
    analyticsApi
      .overview(scope, filter)
      .then((data) => !cancelled && setState({ key, data, failed: false }))
      .catch(() => !cancelled && setState((cur) => ({ key, data: cur?.data ?? null, failed: true })));
    return () => {
      cancelled = true;
    };
  }, [scope, filter, key]);

  const o = state?.data ?? null;
  const loading = state?.key !== key;

  return (
    <div className="flex flex-col gap-4">
      <AnalyticsFilters scope={scope} value={filter} onChange={setFilter} />
      {!o && state?.failed ? (
        <Result
          status="warning"
          title="Không tải được thống kê"
          extra={<Button onClick={() => setVersion((v) => v + 1)}>Thử lại</Button>}
        />
      ) : !o ? (
        <Skeleton active paragraph={{ rows: 12 }} />
      ) : (
        <div className={`flex flex-col gap-4 ${loading ? 'opacity-60' : ''}`}>
          <p className="m-0 flex flex-wrap items-center gap-2 text-small text-neutral-500">
            Kỳ {formatDate(o.period.from)} – {formatDate(o.period.to)} (so với {formatDate(o.period.previousFrom)} –{' '}
            {formatDate(o.period.previousTo)}) · giờ {o.period.zone} · cập nhật {formatTime(o.generatedAt)}
            <Button size="small" type="text" icon={<RefreshCw size={14} />} onClick={() => setVersion((v) => v + 1)}>
              Làm mới
            </Button>
          </p>
          <KpiRow o={o} />

          {/* Admin có 4 ô (thêm Người dùng, Phiên), giáo viên 2 ô → chia đôi cho kín hàng. */}
          <div className={`grid gap-4 sm:grid-cols-2 ${o.users ? 'xl:grid-cols-4' : ''}`}>
            {o.users && (
              <Tile label="Người dùng">
                {Object.entries(o.users.byRole)
                  .map(([r, n]) => `${n} ${ROLE_LABEL[r as Role]?.toLowerCase() ?? r}`)
                  .join(' · ')}
                <br />
                Mới trong kỳ: {o.users.newUsers.current} (
                {signed(delta(o.users.newUsers.current, o.users.newUsers.previous))} so với kỳ trước)
                <br />
                Hoạt động 7 ngày: {o.users.active7Days} · 30 ngày: {o.users.active30Days}
              </Tile>
            )}
            {o.users && (
              <Tile label="Phiên & đăng nhập">
                {o.users.activeSessions} phiên đang hoạt động
                <br />
                Đăng nhập sai trong kỳ: {o.users.failedLogins.current} (kỳ trước {o.users.failedLogins.previous})
              </Tile>
            )}
            <Tile label="Lớp & bài tập">
              {o.classes.withOpenAssignment}/{o.classes.total} lớp có bài đang mở
              <br />
              {o.assignments.length === 0
                ? 'Chưa có bài tập'
                : o.assignments
                    .map((a) => `${a.count} ${KIND_LABEL[a.kind] ?? a.kind} ${STATUS_LABEL[a.status] ?? a.status}`)
                    .join(' · ')}
            </Tile>
            <Tile label="Paper theo nhóm">
              {o.paper.groups} nhóm · {o.paper.studentsWithoutGroup} lượt sinh viên chưa có nhóm
              <br />
              {o.paper.updatesInPeriod} lần sửa tài liệu · {o.paper.liveComments} nhận xét trực tiếp trong kỳ (
              {o.paper.openLiveComments} chưa xử lý)
            </Tile>
          </div>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
            <Card title="Lần nộp theo ngày">
              {o.kpis.submissions.current === 0 ? (
                <NoData text="Chưa có lần nộp nào trong kỳ" />
              ) : (
                <DailyLineChart days={o.submissionsByDay} />
              )}
            </Card>
            <Card
              title="Trạng thái theo bài"
              extra={<span className="text-caption text-neutral-500">12 bài gần hạn nhất</span>}
            >
              {o.byAssignment.length === 0 ? (
                <NoData text="Chưa có bài đã phát hành trong phạm vi lọc" />
              ) : (
                <StatusStackChart rows={o.byAssignment} />
              )}
            </Card>
          </div>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
            <Card title="Phân bố band (bài luận)">
              <Summary s={o.bandSummary} scale="/ 9" />
              {o.bandHistogram.length === 0 ? (
                <NoData text="Chưa có bài luận đã trả" />
              ) : (
                <BandDistributionChart
                  distribution={o.bandHistogram.map((b) => ({ band: b.value, count: b.count }))}
                  unit="kết quả"
                />
              )}
            </Card>
            <Card title="Phân bố điểm Paper (thang 10)">
              <Summary s={o.paperSummary} scale="/ 10" />
              {o.paperHistogram.length === 0 ? (
                <NoData text="Chưa có bài Paper đã trả" />
              ) : (
                <BandDistributionChart
                  distribution={o.paperHistogram.map((b) => ({ band: b.value, count: b.count }))}
                  unit="thành viên"
                  label="Điểm"
                />
              )}
            </Card>
            <Card title="Điểm trung bình theo tiêu chí">
              {o.criteria.length === 0 ? (
                <NoData text="Chưa có điểm theo tiêu chí" />
              ) : (
                <CriteriaBars
                  items={o.criteria.map((c) => ({
                    key: `${c.kind}-${c.name}`,
                    label: c.name,
                    hint: KIND_LABEL[c.kind],
                    average: c.average,
                    count: c.count,
                    max: c.kind === 'PAPER' ? c.maxScore : undefined
                  }))}
                />
              )}
            </Card>
          </div>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
            <Card title="Hoạt động theo giờ trong tuần">
              {o.heatmap.length === 0 ? (
                <NoData text="Chưa có hoạt động trong kỳ" />
              ) : (
                <ActivityHeatmap cells={o.heatmap} zone={o.period.zone} />
              )}
            </Card>
            <Card title="Lỗi hay gặp">
              {o.topErrors.length === 0 ? (
                <NoData text="Chưa có lỗi được đánh dấu" />
              ) : (
                <ol className="m-0 flex list-decimal flex-col gap-1 pl-5 text-small text-neutral-700">
                  {o.topErrors.map((e) => (
                    <li key={e.code}>
                      <span className="font-mono">{e.code}</span> {e.name ?? ''} — {e.count} lần · {e.studentCount} sinh
                      viên
                    </li>
                  ))}
                </ol>
              )}
            </Card>
          </div>

          <StudentAlertsTable scope={scope} filter={filter} />
        </div>
      )}
    </div>
  );
}

function signed(v: number | null): string {
  if (v === null) return '—';
  return v > 0 ? `+${v}` : String(v);
}
