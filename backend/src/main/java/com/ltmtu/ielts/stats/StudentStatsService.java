package com.ltmtu.ielts.stats;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;
import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.assignment.StudentAssignmentAccess;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.grading.BandCalculator;
import com.ltmtu.ielts.grading.BandCalculator.Weighted;
import com.ltmtu.ielts.grading.PaperScoreRules;
import com.ltmtu.ielts.stats.dto.AssignmentStatsDtos.ErrorCount;
import com.ltmtu.ielts.stats.dto.ProgressDtos.StudentRef;
import com.ltmtu.ielts.stats.dto.StudentStatsDtos.AssignmentBand;
import com.ltmtu.ielts.stats.dto.StudentStatsDtos.CriterionCodeAverage;
import com.ltmtu.ielts.stats.dto.StudentStatsDtos.PartBand;
import com.ltmtu.ielts.stats.dto.StudentStatsDtos.StudentStats;

import lombok.RequiredArgsConstructor;

/**
 * Kết quả học tập của một sinh viên (UC-28, FR-64): giáo viên xem trên các bài của mình, sinh viên xem trên các bài
 * mình được thấy. Cùng một cách tính, chỉ khác tập bài tập. Bốn câu SQL cố định, không N+1.
 */
@Service
@RequiredArgsConstructor
public class StudentStatsService {

	/**
	 * Sinh viên theo id. Có {@code :teacherId} thì sinh viên phải "thuộc" giáo viên đó: đang ở một lớp của giáo viên,
	 * hoặc được giao trực tiếp, hoặc đã có bài làm trong bài của giáo viên.
	 */
	private static final String STUDENT_SQL = """
			SELECT u.id, u.student_code, u.full_name FROM users u
			WHERE u.id = :studentId AND u.role = 'STUDENT'
			  AND (CAST(:teacherId AS BIGINT) IS NULL
			       OR EXISTS (SELECT 1 FROM class_members m JOIN classrooms c ON c.id = m.classroom_id
			                  WHERE m.student_id = u.id AND c.teacher_id = :teacherId)
			       OR EXISTS (SELECT 1 FROM assignment_targets t JOIN assignments a ON a.id = t.assignment_id
			                  WHERE t.student_id = u.id AND a.teacher_id = :teacherId)
			       OR EXISTS (SELECT 1 FROM submissions s JOIN assignment_parts p ON p.id = s.part_id
			                  JOIN assignments a ON a.id = p.assignment_id
			                  WHERE s.student_id = u.id AND a.teacher_id = :teacherId))""";

	private static final String TEACHER_ASSIGNMENTS_SQL = "SELECT id FROM assignments WHERE teacher_id = :teacherId";

	private static final String COUNTS_SQL = """
			SELECT count(*) FILTER (WHERE s.current_version > 0) AS submitted, count(*) FILTER (WHERE v.is_late) AS late
			FROM submissions s
			JOIN submission_members sm ON sm.submission_id = s.id
			JOIN assignment_parts p ON p.id = s.part_id
			LEFT JOIN submission_versions v ON v.submission_id = s.id AND v.version_no = 1
			WHERE sm.student_id = :studentId AND p.assignment_id IN (:assignmentIds)""";

	private static final String LATEST = "WITH "
			+ StatsSql.latestReturned("sm.student_id = :studentId AND p.assignment_id IN (:assignmentIds)");

	private static final String RETURNED_SQL = LATEST + """

			SELECT l.assignment_id, a.title AS assignment_title, l.part_id, p.title AS part_title, p.task_type, p.weight,
			       l.overall_score, l.returned_at, l.paper,
			       (SELECT count(*) FROM assignment_parts p2 WHERE p2.assignment_id = l.assignment_id) AS part_count
			FROM latest l
			JOIN assignment_parts p ON p.id = l.part_id
			JOIN assignments a ON a.id = l.assignment_id
			ORDER BY l.returned_at, a.id, p.order_no""";

	private static final String CRITERIA_SQL = LATEST + """

			SELECT rc.code, avg(gcs.score) AS average, count(gcs.score) AS n
			FROM latest l
			JOIN grade_criterion_scores gcs ON gcs.grade_id = l.grade_id
			JOIN rubric_criteria rc ON rc.id = gcs.criterion_id
			WHERE rc.code IS NOT NULL
			GROUP BY rc.code""";

	private static final String ERRORS_SQL = LATEST + " " + StatsSql.TOP_ERRORS;

	private final StudentAssignmentAccess studentAssignmentAccess;
	private final NamedParameterJdbcTemplate jdbc;
	private final Clock clock;

	/** UC-28: kết quả học tập của chính sinh viên, trên các bài sinh viên được thấy (BR-04, BR-05). */
	@Transactional(readOnly = true)
	public StudentStats mine(Long studentId) {
		List<Long> assignmentIds = studentAssignmentAccess.visibleAssignments(studentId, clock.instant()).stream()
				.map(Assignment::getId).toList();
		return build(requireStudent(studentId, null), assignmentIds);
	}

	/**
	 * FR-64: giáo viên xem một sinh viên, chỉ trên các bài của mình. Sinh viên không thuộc lớp / bài nào của giáo viên
	 * → 404 (không lộ việc tài khoản tồn tại).
	 */
	@Transactional(readOnly = true)
	public StudentStats ofTeacher(Long teacherId, Long studentId) {
		StudentRef student = requireStudent(studentId, teacherId);
		List<Long> assignmentIds = jdbc.queryForList(TEACHER_ASSIGNMENTS_SQL,
				new MapSqlParameterSource("teacherId", teacherId), Long.class);
		return build(student, assignmentIds);
	}

	private StudentRef requireStudent(Long studentId, Long teacherId) {
		MapSqlParameterSource params = new MapSqlParameterSource("studentId", studentId)
				.addValue("teacherId", teacherId, Types.BIGINT);
		return jdbc.query(STUDENT_SQL, params,
				(rs, i) -> new StudentRef(rs.getLong("id"), rs.getString("student_code"), rs.getString("full_name")))
				.stream().findFirst()
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy sinh viên"));
	}

	private StudentStats build(StudentRef student, Collection<Long> assignmentIds) {
		if (assignmentIds.isEmpty()) {
			return new StudentStats(student, 0, 0, 0, null, List.of(), List.of(), List.of(), List.of());
		}
		MapSqlParameterSource params = new MapSqlParameterSource("studentId", student.id())
				.addValue("assignmentIds", assignmentIds)
				.addValue("limit", StatsSql.TOP_ERROR_LIMIT);

		int[] counts = jdbc.queryForObject(COUNTS_SQL, params,
				(rs, i) -> new int[] { rs.getInt("submitted"), rs.getInt("late") });

		List<PartBand> parts = new ArrayList<>();
		Map<Long, AssignmentAcc> byAssignment = new LinkedHashMap<>();
		jdbc.query(RETURNED_SQL, params, rs -> {
			BigDecimal raw = rs.getBigDecimal("overall_score");
			Instant returnedAt = StatsSql.instant(rs, "returned_at");
			long assignmentId = rs.getLong("assignment_id");
			String title = rs.getString("assignment_title");
			boolean paper = rs.getBoolean("paper");
			parts.add(new PartBand(assignmentId, title, rs.getLong("part_id"), rs.getString("part_title"),
					TaskType.valueOf(rs.getString("task_type")), PaperScoreRules.display(raw, paper), returnedAt, paper));
			AssignmentAcc acc = byAssignment.computeIfAbsent(assignmentId,
					id -> new AssignmentAcc(title, paper, new ArrayList<>()));
			acc.partCount = rs.getInt("part_count");
			acc.parts.add(new Weighted(raw, rs.getBigDecimal("weight")));
			acc.returnedAt = returnedAt;
		});

		List<AssignmentBand> assignments = new ArrayList<>();
		List<BigDecimal> completeBands = new ArrayList<>();
		byAssignment.forEach((id, acc) -> {
			boolean complete = acc.parts.size() == acc.partCount;
			BigDecimal raw = complete ? BandCalculator.assignmentBand(acc.parts) : null;
			// Bài PAPER thang 10 không gộp vào band trung bình IELTS.
			if (raw != null && !acc.paper) {
				completeBands.add(raw);
			}
			assignments.add(new AssignmentBand(id, acc.title, acc.partCount, acc.parts.size(), complete,
					PaperScoreRules.display(raw, acc.paper), acc.returnedAt, acc.paper));
		});
		assignments.sort(Comparator.comparing(AssignmentBand::returnedAt));

		Map<CriterionCode, CriterionCodeAverage> criteria = new EnumMap<>(CriterionCode.class);
		jdbc.query(CRITERIA_SQL, params, rs -> {
			CriterionCode code = CriterionCode.valueOf(rs.getString("code"));
			criteria.put(code, new CriterionCodeAverage(code, StatsMath.round(rs.getBigDecimal("average")), rs.getInt("n")));
		});
		List<ErrorCount> errors = jdbc.query(ERRORS_SQL, params, StatsSql.ERROR_COUNT);

		return new StudentStats(student, counts[0], counts[1], parts.size(), StatsMath.average(completeBands),
				assignments, parts, List.copyOf(criteria.values()), errors);
	}

	/** Gom các phần đã trả của một bài; dòng đến theo thời điểm trả tăng dần nên {@code returnedAt} là lần trả cuối. */
	private static final class AssignmentAcc {
		final String title;
		final boolean paper;
		final List<Weighted> parts;
		int partCount;
		Instant returnedAt;

		AssignmentAcc(String title, boolean paper, List<Weighted> parts) {
			this.title = title;
			this.paper = paper;
			this.parts = parts;
		}
	}
}
