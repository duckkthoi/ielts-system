package com.ltmtu.ielts.stats.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Thống kê tổng hợp có bộ lọc (H5) — docs/05 mục A.9b. Admin xem toàn hệ thống, giáo viên chỉ lớp / bài của mình.
 * Mọi tỉ lệ đều kèm tử số / mẫu số ({@link Ratio}); "ngày" tính theo {@code zone}.
 */
public final class AnalyticsDtos {

	private AnalyticsDtos() {
	}

	/** Kỳ đang xem [from, to] (gồm cả 2 ngày) và kỳ liền trước cùng độ dài để so sánh. */
	public record Period(LocalDate from, LocalDate to, LocalDate previousFrom, LocalDate previousTo, String zone) {
	}

	/** Tỉ lệ = {@code numerator / denominator}; mẫu số 0 → {@code rate = null} (không đủ dữ liệu). */
	public record Ratio(long numerator, long denominator, BigDecimal rate) {
	}

	/** Một chỉ số của kỳ này và kỳ trước. */
	public record Compare<T>(T current, T previous) {
	}

	/** Chỉ admin: người dùng, phiên, đăng nhập sai. */
	public record UserBlock(Map<String, Long> byRole, Compare<Long> newUsers, long active7Days, long active30Days,
			long activeSessions, Compare<Long> failedLogins) {
	}

	/** {@code withOpenAssignment}: lớp đang có ít nhất một bài đang mở giao cho lớp; còn lại coi là không hoạt động. */
	public record ClassBlock(long total, long withOpenAssignment) {
	}

	public record KindStatusCount(String kind, String status, long count) {
	}

	/**
	 * Mỗi đơn vị = (sinh viên nhận bài, phần). Thành viên nhóm Paper tính riêng từng người, mỗi người một lần. Nháp
	 * không tính là đã nộp; {@code overdue} = chưa nộp (chưa làm / đang làm) và đã quá hạn của phần.
	 */
	public record UnitBlock(long total, long notStarted, long inProgress, long submitted, long revision, long returned,
			long overdue, Ratio completion) {
	}

	public record KpiBlock(Compare<Long> submissions, Compare<Ratio> onTime, Compare<BigDecimal> avgBand,
			Compare<BigDecimal> avgPaperScore) {
	}

	public record DailyCount(LocalDate date, long count) {
	}

	/** Phân bố trạng thái của một bài (biểu đồ cột chồng). */
	public record AssignmentStatusRow(Long assignmentId, String title, String kind, long notStarted, long inProgress,
			long submitted, long revision, long returned, long overdue) {
	}

	public record ScoreSummary(long count, BigDecimal mean, BigDecimal median, BigDecimal min, BigDecimal max) {
	}

	public record Bucket(BigDecimal value, long count) {
	}

	public record CriterionAvg(String kind, String name, BigDecimal average, BigDecimal maxScore, long count) {
	}

	/** Ô của bản đồ nhiệt: {@code dow} 1 = thứ Hai … 7 = Chủ nhật, {@code hour} 0–23 theo {@code zone}. */
	public record HeatCell(int dow, int hour, long count) {
	}

	public record PaperBlock(long groups, long studentsWithoutGroup, long updatesInPeriod, long liveComments,
			long openLiveComments) {
	}

	public record Overview(Period period, UserBlock users, ClassBlock classes, List<KindStatusCount> assignments,
			UnitBlock units, KpiBlock kpis, List<DailyCount> submissionsByDay, List<AssignmentStatusRow> byAssignment,
			ScoreSummary bandSummary, List<Bucket> bandHistogram, ScoreSummary paperSummary,
			List<Bucket> paperHistogram, List<CriterionAvg> criteria, List<HeatCell> heatmap, List<AssignmentStatsDtos.ErrorCount> topErrors,
			PaperBlock paper, Instant generatedAt) {
	}

	/**
	 * Một sinh viên trong phạm vi lọc (bảng cảnh báo / xuất file). {@code revisions}: số lần nộp lại + phần đang "Cần
	 * sửa"; {@code progress}: band gần nhất − band đầu tiên (bài luận đã trả).
	 */
	public record StudentRow(Long studentId, String studentCode, String fullName, String classes, long assigned,
			long submitted, long overdue, long revisions, BigDecimal latestBand, BigDecimal progress,
			Instant lastActivityAt, List<String> alerts) {
	}
}
