package com.ltmtu.ielts.stats;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.assignment.RubricCriterion;
import com.ltmtu.ielts.assignment.TeacherAssignmentAccess;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.grading.BandCalculator;
import com.ltmtu.ielts.grading.BandCalculator.Weighted;
import com.ltmtu.ielts.stats.dto.AssignmentStatsDtos.AssignmentStats;
import com.ltmtu.ielts.stats.dto.AssignmentStatsDtos.CriterionAverage;
import com.ltmtu.ielts.stats.dto.AssignmentStatsDtos.ErrorCount;
import com.ltmtu.ielts.stats.dto.AssignmentStatsDtos.OverallStats;
import com.ltmtu.ielts.stats.dto.AssignmentStatsDtos.PartStats;

import lombok.RequiredArgsConstructor;

/**
 * Thống kê một bài tập (UC-19, FR-63). Bốn câu SQL cố định (số nộp, kết quả đã trả, điểm tiêu chí, mã lỗi) trên
 * cùng tập sinh viên và cùng kết quả chấm ({@link StatsSql}) — không phụ thuộc số sinh viên, không N+1.
 */
@Service
@RequiredArgsConstructor
public class AssignmentStatsService {

	private static final String STUDENTS_SQL = "WITH " + StatsSql.PEOPLE + " SELECT count(*) FROM people";

	/** Theo từng phần: số đã nộp, số có lần nộp đầu muộn. */
	private static final String SUBMITTED_SQL = "WITH " + StatsSql.PEOPLE + """

			SELECT s.part_id, count(*) AS submitted, count(*) FILTER (WHERE v.is_late) AS late
			FROM submissions s
			JOIN assignment_parts p ON p.id = s.part_id
			JOIN submission_versions v ON v.submission_id = s.id AND v.version_no = 1
			WHERE p.assignment_id = :assignmentId AND s.student_id IN (SELECT student_id FROM people)
			GROUP BY s.part_id""";

	private static final String RETURNED_SQL = "WITH " + StatsSql.PEOPLE + ", " + StatsSql.LATEST_RETURNED
			+ " SELECT student_id, part_id, overall_score FROM latest";

	private static final String CRITERIA_SQL = "WITH " + StatsSql.PEOPLE + ", " + StatsSql.LATEST_RETURNED + """

			SELECT gcs.criterion_id, avg(gcs.score) AS average, count(gcs.score) AS n
			FROM latest l JOIN grade_criterion_scores gcs ON gcs.grade_id = l.grade_id
			GROUP BY gcs.criterion_id""";

	private static final String ERRORS_SQL = "WITH " + StatsSql.PEOPLE + ", " + StatsSql.LATEST_RETURNED + " "
			+ StatsSql.TOP_ERRORS;

	private final TeacherAssignmentAccess assignmentAccess;
	private final ClassroomService classroomService;
	private final NamedParameterJdbcTemplate jdbc;

	/**
	 * UC-19: thống kê một bài, tuỳ chọn chỉ một lớp. Bài của giáo viên khác → 404; lớp không phải của mình → 400.
	 * Tập sinh viên giống ma trận tiến độ. Chỉ tính kết quả đã trả (lấy version mới nhất có kết quả đã trả), kể cả
	 * "Chấm 0".
	 */
	@Transactional(readOnly = true)
	public AssignmentStats stats(Long teacherId, Long assignmentId, Long classId) {
		Assignment a = assignmentAccess.requireOwned(teacherId, assignmentId);
		if (classId != null) {
			classroomService.requireOwnedClassrooms(teacherId, List.of(classId));
		}
		MapSqlParameterSource params = StatsSql.params(assignmentId, classId)
				.addValue("limit", StatsSql.TOP_ERROR_LIMIT);

		Integer students = jdbc.queryForObject(STUDENTS_SQL, params, Integer.class);
		Map<Long, int[]> submitted = new HashMap<>();
		jdbc.query(SUBMITTED_SQL, params,
				rs -> {
					submitted.put(rs.getLong("part_id"), new int[] { rs.getInt("submitted"), rs.getInt("late") });
				});
		// band phần (chưa làm tròn) theo phần → theo sinh viên
		Map<Long, Map<Long, BigDecimal>> bands = new HashMap<>();
		jdbc.query(RETURNED_SQL, params,
				rs -> {
					bands.computeIfAbsent(rs.getLong("part_id"), k -> new HashMap<>())
							.put(rs.getLong("student_id"), rs.getBigDecimal("overall_score"));
				});
		Map<Long, CriterionRow> criteria = new HashMap<>();
		jdbc.query(CRITERIA_SQL, params,
				rs -> {
					criteria.put(rs.getLong("criterion_id"), new CriterionRow(rs.getBigDecimal("average"), rs.getInt("n")));
				});
		List<ErrorCount> topErrors = jdbc.query(ERRORS_SQL, params, StatsSql.ERROR_COUNT);

		List<PartStats> perPart = new ArrayList<>();
		for (AssignmentPart p : a.getParts()) {
			int[] counts = submitted.getOrDefault(p.getId(), new int[2]);
			Collection<BigDecimal> partBands = bands.getOrDefault(p.getId(), Map.of()).values();
			List<CriterionAverage> criterionAverages = new ArrayList<>();
			for (RubricCriterion c : p.getCriteria()) {
				CriterionRow row = criteria.get(c.getId());
				criterionAverages.add(new CriterionAverage(c.getId(), c.getCode(), c.getName(),
						row == null ? null : StatsMath.round(row.average()), row == null ? 0 : row.count()));
			}
			perPart.add(new PartStats(p.getId(), p.getOrderNo(), p.getTitle(), p.getTaskType(), counts[0], counts[1],
					counts[0] == 0 ? null : (double) (counts[0] - counts[1]) / counts[0], partBands.size(),
					StatsMath.average(partBands), criterionAverages, StatsMath.distribution(partBands)));
		}
		return new AssignmentStats(students == null ? 0 : students, perPart, overall(a, bands), topErrors);
	}

	/** BR-12: band cả bài của từng sinh viên đã được trả mọi phần (trọng số theo phần). */
	private static OverallStats overall(Assignment a, Map<Long, Map<Long, BigDecimal>> bands) {
		Map<Long, List<Weighted>> byStudent = new HashMap<>();
		for (AssignmentPart p : a.getParts()) {
			bands.getOrDefault(p.getId(), Map.of()).forEach((studentId, band) -> byStudent
					.computeIfAbsent(studentId, k -> new ArrayList<>()).add(new Weighted(band, p.getWeight())));
		}
		List<BigDecimal> overall = new ArrayList<>();
		int partCount = a.getParts().size();
		for (List<Weighted> parts : byStudent.values()) {
			if (parts.size() == partCount) {
				overall.add(BandCalculator.assignmentBand(parts));
			}
		}
		return new OverallStats(overall.size(), StatsMath.average(overall), StatsMath.distribution(overall));
	}

	private record CriterionRow(BigDecimal average, int count) {
	}
}
