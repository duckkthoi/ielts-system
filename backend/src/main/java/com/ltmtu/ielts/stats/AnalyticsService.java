package com.ltmtu.ielts.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.TeacherAssignmentAccess;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.AssignmentStatusRow;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.Bucket;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.ClassBlock;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.Compare;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.CriterionAvg;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.DailyCount;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.HeatCell;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.KindStatusCount;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.KpiBlock;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.Overview;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.PaperBlock;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.Period;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.Ratio;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.ScoreSummary;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.StudentRow;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.UnitBlock;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.UserBlock;

import lombok.RequiredArgsConstructor;

/**
 * Thống kê tổng hợp có bộ lọc (H5): tính hoàn toàn bằng SQL tổng hợp ({@link AnalyticsSql}), không tải bài nộp về.
 * Admin xem toàn hệ thống (lọc được theo giáo viên, kèm khối người dùng / phiên); giáo viên chỉ thấy bài / lớp của mình
 * — lớp của người khác → 400 (như màn giám sát), bài của người khác → 404.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

	/** Kỳ mặc định khi không chọn ngày: 30 ngày gần nhất. Tối đa 366 ngày. */
	static final int DEFAULT_DAYS = 30;
	static final int MAX_DAYS = 366;
	/** Không hoạt động ≥ 7 ngày → cảnh báo. */
	static final int INACTIVE_DAYS = 7;
	/** Số dòng tối đa khi xuất file. */
	static final int EXPORT_LIMIT = 5_000;

	private final NamedParameterJdbcTemplate jdbc;
	private final Clock clock;
	private final AppProperties props;
	private final TeacherAssignmentAccess assignmentAccess;
	private final ClassroomService classroomService;

	/** Bộ lọc đã kiểm tra quyền; {@code teacherId} null = mọi giáo viên (chỉ admin). */
	public record Filter(boolean admin, Long teacherId, Long classId, Long assignmentId, AssignmentKind kind,
			AssignmentStatus status, LocalDate from, LocalDate to) {
	}

	/** Kiểm quyền + chuẩn hoá kỳ. Giáo viên chỉ lọc được lớp / bài của mình; admin lọc được theo giáo viên. */
	@Transactional(readOnly = true)
	public Filter filter(Long teacherId, Long classId, Long assignmentId, AssignmentKind kind, AssignmentStatus status,
			LocalDate from, LocalDate to, boolean admin) {
		if (!admin) {
			if (classId != null) {
				classroomService.requireOwnedClassrooms(teacherId, List.of(classId));
			}
			if (assignmentId != null) {
				assignmentAccess.requireOwned(teacherId, assignmentId);
			}
		}
		if (status == AssignmentStatus.DRAFT) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Thống kê chỉ tính bài đã phát hành");
		}
		LocalDate today = LocalDate.now(clock.withZone(props.zone()));
		LocalDate end = to == null ? today : to;
		LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS - 1L) : from;
		if (start.isAfter(end)) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
		}
		if (ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Chỉ xem được tối đa " + MAX_DAYS + " ngày một lần");
		}
		return new Filter(admin, teacherId, classId, assignmentId, kind, status, start, end);
	}

	@Transactional(readOnly = true)
	public Overview overview(Filter f) {
		ZoneId zone = props.zone();
		Instant now = clock.instant();
		long days = ChronoUnit.DAYS.between(f.from(), f.to()) + 1;
		LocalDate prevTo = f.from().minusDays(1);
		LocalDate prevFrom = prevTo.minusDays(days - 1);
		MapSqlParameterSource cur = params(f, f.from(), f.to());
		MapSqlParameterSource prev = params(f, prevFrom, prevTo);

		long[] vNow = versionCounts(cur);
		long[] vPrev = versionCounts(prev);
		BigDecimal[] avgNow = averages(cur);
		BigDecimal[] avgPrev = averages(prev);
		KpiBlock kpis = new KpiBlock(new Compare<>(vNow[0], vPrev[0]),
				new Compare<>(ratio(vNow[0] - vNow[1], vNow[0]), ratio(vPrev[0] - vPrev[1], vPrev[0])),
				new Compare<>(avgNow[0], avgPrev[0]), new Compare<>(avgNow[1], avgPrev[1]));

		return new Overview(new Period(f.from(), f.to(), prevFrom, prevTo, zone.getId()),
				f.admin() ? users(cur, prev, now) : null, classes(cur), assignments(cur), units(cur), kpis,
				jdbc.query(AnalyticsSql.BY_DAY, cur,
						(rs, i) -> new DailyCount(rs.getObject("day", LocalDate.class), rs.getLong("total"))),
				jdbc.query(AnalyticsSql.BY_ASSIGNMENT, cur, (rs, i) -> new AssignmentStatusRow(rs.getLong("id"),
						rs.getString("title"), rs.getString("kind"), rs.getLong("not_started"), rs.getLong("in_progress"),
						rs.getLong("submitted"), rs.getLong("revision"), rs.getLong("returned"), rs.getLong("overdue"))),
				scoreSummary(cur, false), histogram(cur, false), scoreSummary(cur, true), histogram(cur, true),
				jdbc.query(AnalyticsSql.CRITERIA, cur, (rs, i) -> new CriterionAvg(rs.getString("kind"),
						rs.getString("name"), rs.getBigDecimal("average"), rs.getBigDecimal("max_score"), rs.getLong("n"))),
				jdbc.query(AnalyticsSql.HEATMAP, cur,
						(rs, i) -> new HeatCell(rs.getInt("dow"), rs.getInt("hr"), rs.getLong("n"))),
				jdbc.query(AnalyticsSql.TOP_ERRORS, cur.addValue("limit", StatsSql.TOP_ERROR_LIMIT), StatsSql.ERROR_COUNT),
				paper(cur), now);
	}

	/** Bảng sinh viên (cảnh báo) có phân trang. */
	@Transactional(readOnly = true)
	public Page<StudentRow> students(Filter f, boolean alertsOnly, int page, int size) {
		int safeSize = Math.clamp(size, 1, 100);
		int safePage = Math.max(page, 0);
		long[] total = { 0 };
		List<StudentRow> rows = studentRows(f, alertsOnly, safeSize, (long) safePage * safeSize, total);
		return new PageImpl<>(rows, PageRequest.of(safePage, safeSize), total[0]);
	}

	/** Mọi sinh viên trong phạm vi (xuất file), tối đa {@value #EXPORT_LIMIT} dòng. */
	@Transactional(readOnly = true)
	public List<StudentRow> allStudents(Filter f) {
		return studentRows(f, false, EXPORT_LIMIT, 0, new long[1]);
	}

	// ─── Nội bộ ──────────────────────────────────────────────────────────────

	private List<StudentRow> studentRows(Filter f, boolean alertsOnly, int limit, long offset, long[] total) {
		Instant inactiveBefore = clock.instant().minus(Duration.ofDays(INACTIVE_DAYS));
		MapSqlParameterSource p = params(f, f.from(), f.to()).addValue("alertsOnly", alertsOnly)
				.addValue("limit", limit).addValue("offset", offset)
				.addValue("inactiveBefore", Timestamp.from(inactiveBefore));
		return jdbc.query(AnalyticsSql.STUDENTS, p, (rs, i) -> {
			total[0] = rs.getLong("total");
			List<String> alerts = new ArrayList<>();
			if (rs.getLong("overdue") > 0) {
				alerts.add("Quá hạn " + rs.getLong("overdue") + " phần");
			}
			if (rs.getBoolean("inactive")) {
				alerts.add("Không hoạt động ≥ " + INACTIVE_DAYS + " ngày");
			}
			if (rs.getLong("revisions") >= 2) {
				alerts.add("Sửa bài " + rs.getLong("revisions") + " lần");
			}
			return new StudentRow(rs.getLong("student_id"), rs.getString("student_code"), rs.getString("full_name"),
					rs.getString("classes"), rs.getLong("assigned"), rs.getLong("submitted"), rs.getLong("overdue"),
					rs.getLong("revisions"), rs.getBigDecimal("latest"), rs.getBigDecimal("progress"),
					StatsSql.instant(rs, "last_activity"), alerts);
		});
	}

	private MapSqlParameterSource params(Filter f, LocalDate from, LocalDate to) {
		ZoneId zone = props.zone();
		return new MapSqlParameterSource()
				.addValue("teacherId", f.teacherId(), Types.BIGINT)
				.addValue("classId", f.classId(), Types.BIGINT)
				.addValue("assignmentId", f.assignmentId(), Types.BIGINT)
				.addValue("kind", f.kind() == null ? null : f.kind().name(), Types.VARCHAR)
				.addValue("status", f.status() == null ? null : f.status().name(), Types.VARCHAR)
				.addValue("now", Timestamp.from(clock.instant()))
				.addValue("from", Timestamp.from(from.atStartOfDay(zone).toInstant()))
				.addValue("to", Timestamp.from(to.plusDays(1).atStartOfDay(zone).toInstant()))
				.addValue("fromDate", Date.valueOf(from))
				.addValue("toDate", Date.valueOf(to))
				.addValue("zone", zone.getId());
	}

	private long[] versionCounts(MapSqlParameterSource p) {
		return jdbc.queryForObject(AnalyticsSql.VERSION_COUNTS, p,
				(rs, i) -> new long[] { rs.getLong("total"), rs.getLong("late") });
	}

	private BigDecimal[] averages(MapSqlParameterSource p) {
		return jdbc.queryForObject(AnalyticsSql.AVG_RETURNED, p,
				(rs, i) -> new BigDecimal[] { rs.getBigDecimal("band"), rs.getBigDecimal("paper") });
	}

	private UnitBlock units(MapSqlParameterSource p) {
		return jdbc.queryForObject(AnalyticsSql.UNIT_COUNTS, p, (rs, i) -> {
			long total = rs.getLong("total");
			long done = rs.getLong("submitted") + rs.getLong("revision") + rs.getLong("returned");
			return new UnitBlock(total, rs.getLong("not_started"), rs.getLong("in_progress"), rs.getLong("submitted"),
					rs.getLong("revision"), rs.getLong("returned"), rs.getLong("overdue"), ratio(done, total));
		});
	}

	private ScoreSummary scoreSummary(MapSqlParameterSource p, boolean paper) {
		return jdbc.queryForObject(AnalyticsSql.SCORE_SUMMARY, new MapSqlParameterSource(p.getValues()).addValue("paper",
				paper), (rs, i) -> new ScoreSummary(rs.getLong("n"), rs.getBigDecimal("mean"), rs.getBigDecimal("median"),
						rs.getBigDecimal("lo"), rs.getBigDecimal("hi")));
	}

	private List<Bucket> histogram(MapSqlParameterSource p, boolean paper) {
		return jdbc.query(AnalyticsSql.HISTOGRAM, new MapSqlParameterSource(p.getValues()).addValue("paper", paper),
				(rs, i) -> new Bucket(rs.getBigDecimal("bucket"), rs.getLong("n")));
	}

	private List<KindStatusCount> assignments(MapSqlParameterSource p) {
		return jdbc.query(AnalyticsSql.ASSIGNMENTS, p,
				(rs, i) -> new KindStatusCount(rs.getString("kind"), rs.getString("status"), rs.getLong("n")));
	}

	private ClassBlock classes(MapSqlParameterSource p) {
		return jdbc.queryForObject(AnalyticsSql.CLASSES, p,
				(rs, i) -> new ClassBlock(rs.getLong("total"), rs.getLong("open")));
	}

	private PaperBlock paper(MapSqlParameterSource p) {
		return jdbc.queryForObject(AnalyticsSql.PAPER, p, (rs, i) -> new PaperBlock(rs.getLong("groups"),
				rs.getLong("without_group"), rs.getLong("updates"), rs.getLong("comments"), rs.getLong("open_comments")));
	}

	/** Khối người dùng (chỉ admin). Người hoạt động = có phiên dùng gần đây, mỗi người đếm một lần. */
	private UserBlock users(MapSqlParameterSource cur, MapSqlParameterSource prev, Instant now) {
		Map<String, Long> byRole = new LinkedHashMap<>();
		for (String r : List.of("ADMIN", "TEACHER", "STUDENT")) {
			byRole.put(r, 0L);
		}
		jdbc.query("SELECT role, count(*) AS n FROM users GROUP BY role", Map.of(),
				rs -> {
					byRole.put(rs.getString("role"), rs.getLong("n"));
				});
		String newUsers = "SELECT count(*) FROM users WHERE created_at >= :from AND created_at < :to";
		String failed = "SELECT count(*) FROM audit_logs WHERE action = 'LOGIN_FAILED' AND created_at >= :from "
				+ "AND created_at < :to";
		String active = "SELECT count(DISTINCT user_id) FROM user_sessions WHERE last_used_at >= :since";
		return new UserBlock(byRole, new Compare<>(count(newUsers, cur), count(newUsers, prev)),
				count(active, new MapSqlParameterSource("since", Timestamp.from(now.minus(Duration.ofDays(7))))),
				count(active, new MapSqlParameterSource("since", Timestamp.from(now.minus(Duration.ofDays(30))))),
				count("SELECT count(*) FROM user_sessions WHERE revoked_at IS NULL AND expires_at > :now", cur),
				new Compare<>(count(failed, cur), count(failed, prev)));
	}

	private long count(String sql, MapSqlParameterSource p) {
		Long n = jdbc.queryForObject(sql, p, Long.class);
		return n == null ? 0 : n;
	}

	static Ratio ratio(long numerator, long denominator) {
		return new Ratio(numerator, denominator, denominator == 0 ? null
				: BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP));
	}
}
