package com.ltmtu.ielts.stats;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.user.Role;

import lombok.RequiredArgsConstructor;

/**
 * Thống kê toàn hệ thống cho admin (UC-09). Truy vấn tổng hợp trên nhiều bảng nên dùng SQL trực tiếp
 * ({@link JdbcTemplate}) thay vì tải entity.
 */
@Service
@RequiredArgsConstructor
public class SystemStatsService {

	/** Số ngày của biểu đồ "Bài nộp theo ngày". */
	static final int DAYS = 14;

	private final JdbcTemplate jdbc;
	private final Clock clock;
	private final AppProperties props;

	@Transactional(readOnly = true)
	public SystemStats stats() {
		ZoneId zone = props.zone();
		LocalDate today = LocalDate.now(clock.withZone(zone));

		Map<Role, Long> byRole = new EnumMap<>(Role.class);
		for (Role r : Role.values()) {
			byRole.put(r, 0L);
		}
		jdbc.query("SELECT role, count(*) FROM users GROUP BY role",
				rs -> {
					byRole.put(Role.valueOf(rs.getString(1)), rs.getLong(2));
				});

		Map<String, Long> assignmentsByStatus = new LinkedHashMap<>();
		for (String s : List.of("DRAFT", "PUBLISHED", "CLOSED")) {
			assignmentsByStatus.put(s, 0L);
		}
		jdbc.query("SELECT status, count(*) FROM assignments GROUP BY status",
				rs -> {
					assignmentsByStatus.put(rs.getString(1), rs.getLong(2));
				});

		List<DailyCount> byDay = jdbc.query("""
				SELECT d::date AS day, count(v.id) AS total
				FROM generate_series(?::date - (? - 1), ?::date, interval '1 day') AS d
				LEFT JOIN submission_versions v ON (v.submitted_at AT TIME ZONE ?)::date = d::date
				GROUP BY d ORDER BY d""",
				(rs, i) -> new DailyCount(rs.getObject("day", LocalDate.class), rs.getLong("total")),
				Date.valueOf(today), DAYS, Date.valueOf(today), zone.getId());

		return new SystemStats(
				byRole,
				count("SELECT count(*) FROM users WHERE NOT active"),
				count("SELECT count(*) FROM classrooms"),
				assignmentsByStatus,
				byDay.isEmpty() ? 0 : byDay.getLast().count(),
				byDay,
				count("SELECT count(*) FROM user_sessions WHERE revoked_at IS NULL AND expires_at > now()"));
	}

	private long count(String sql) {
		Long n = jdbc.queryForObject(sql, Long.class);
		return n == null ? 0 : n;
	}

	public record SystemStats(
			Map<Role, Long> usersByRole,
			long lockedUsers,
			long classrooms,
			Map<String, Long> assignmentsByStatus,
			long submissionsToday,
			List<DailyCount> submissionsByDay,
			long activeSessions) {
	}

	public record DailyCount(LocalDate date, long count) {
	}
}
