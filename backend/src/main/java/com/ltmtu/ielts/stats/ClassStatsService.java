package com.ltmtu.ielts.stats;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.ClassroomSummary;
import com.ltmtu.ielts.grading.BandCalculator;
import com.ltmtu.ielts.grading.BandCalculator.Weighted;
import com.ltmtu.ielts.stats.dto.ClassStatsDtos.ClassAssignment;
import com.ltmtu.ielts.stats.dto.ClassStatsDtos.ClassInfo;
import com.ltmtu.ielts.stats.dto.ClassStatsDtos.ClassStats;
import com.ltmtu.ielts.stats.dto.ClassStatsDtos.RankingRow;
import com.ltmtu.ielts.stats.dto.ProgressDtos.StudentRef;

import lombok.RequiredArgsConstructor;

/**
 * Thống kê một lớp và bảng xếp hạng (UC-19, FR-65). Bốn câu SQL cố định (thành viên, bài của lớp, số nộp, kết quả
 * đã trả), phần gộp làm trong Java — không N+1.
 */
@Service
@RequiredArgsConstructor
public class ClassStatsService {

	private static final String MEMBERS_SQL = """
			SELECT u.id, u.student_code, u.full_name FROM class_members m JOIN users u ON u.id = m.student_id
			WHERE m.classroom_id = :classId ORDER BY u.student_code, u.id""";

	/** Bài của giáo viên giao cho lớp (giao theo lớp, không tính bài chỉ giao trực tiếp cho từng người). */
	private static final String CLASS_ASSIGNMENTS = """
			SELECT a.id FROM assignments a JOIN assignment_targets t ON t.assignment_id = a.id
			WHERE t.classroom_id = :classId AND a.teacher_id = :teacherId AND a.status <> 'DRAFT'""";

	private static final String ASSIGNMENTS_SQL = """
			SELECT a.id, a.title, a.status, a.due_at, a.kind,
			       (SELECT count(*) FROM assignment_parts p WHERE p.assignment_id = a.id) AS part_count
			FROM assignments a WHERE a.id IN (""" + CLASS_ASSIGNMENTS + """
			)
			ORDER BY a.due_at NULLS LAST, a.id""";

	private static final String MEMBER_FILTER = "sm.student_id IN (SELECT student_id FROM class_members WHERE classroom_id = :classId)";

	/** Theo (sinh viên, bài): số phần đã nộp, số phần có lần nộp đầu muộn (bài PAPER: mọi thành viên nhóm). */
	private static final String SUBMITTED_SQL = """
			SELECT sm.student_id, p.assignment_id,
			       count(*) FILTER (WHERE s.current_version > 0) AS submitted, count(*) FILTER (WHERE v.is_late) AS late
			FROM submissions s
			JOIN submission_members sm ON sm.submission_id = s.id
			JOIN assignment_parts p ON p.id = s.part_id
			LEFT JOIN submission_versions v ON v.submission_id = s.id AND v.version_no = 1
			WHERE p.assignment_id IN (""" + CLASS_ASSIGNMENTS + ") AND " + MEMBER_FILTER + """

			GROUP BY sm.student_id, p.assignment_id""";

	private static final String RETURNED_SQL = "WITH "
			+ StatsSql.latestReturned("p.assignment_id IN (" + CLASS_ASSIGNMENTS + ") AND " + MEMBER_FILTER) + """

			SELECT l.student_id, l.assignment_id, l.overall_score, p.weight
			FROM latest l JOIN assignment_parts p ON p.id = l.part_id""";

	private final ClassroomService classroomService;
	private final NamedParameterJdbcTemplate jdbc;

	/** UC-19 / FR-65: thống kê lớp của giáo viên. Lớp của người khác → 404. */
	@Transactional(readOnly = true)
	public ClassStats stats(Long teacherId, Long classId) {
		ClassroomSummary c = classroomService.detail(teacherId, classId);
		MapSqlParameterSource params = new MapSqlParameterSource("classId", classId).addValue("teacherId", teacherId);

		List<StudentRef> members = jdbc.query(MEMBERS_SQL, params,
				(rs, i) -> new StudentRef(rs.getLong("id"), rs.getString("student_code"), rs.getString("full_name")));
		Map<Long, AssignmentRow> assignments = new LinkedHashMap<>();
		jdbc.query(ASSIGNMENTS_SQL, params, rs -> {
			assignments.put(rs.getLong("id"), new AssignmentRow(rs.getString("title"),
					AssignmentStatus.valueOf(rs.getString("status")), StatsSql.instant(rs, "due_at"), rs.getInt("part_count"),
					AssignmentKind.valueOf(rs.getString("kind"))));
		});
		// (sinh viên, bài) → [đã nộp, muộn]
		Map<Key, int[]> submitted = new HashMap<>();
		jdbc.query(SUBMITTED_SQL, params, rs -> {
			submitted.put(new Key(rs.getLong("student_id"), rs.getLong("assignment_id")),
					new int[] { rs.getInt("submitted"), rs.getInt("late") });
		});
		// (sinh viên, bài) → band các phần đã trả kèm trọng số
		Map<Key, List<Weighted>> returned = new HashMap<>();
		jdbc.query(RETURNED_SQL, params, rs -> {
			returned.computeIfAbsent(new Key(rs.getLong("student_id"), rs.getLong("assignment_id")), k -> new ArrayList<>())
					.add(new Weighted(rs.getBigDecimal("overall_score"), rs.getBigDecimal("weight")));
		});

		// Band cả bài (BR-12) của từng (sinh viên, bài) đã đủ phần.
		Map<Key, BigDecimal> bands = new HashMap<>();
		returned.forEach((k, parts) -> {
			AssignmentRow a = assignments.get(k.assignmentId());
			if (a != null && parts.size() == a.partCount()) {
				bands.put(k, BandCalculator.assignmentBand(parts));
			}
		});

		List<ClassAssignment> assignmentStats = new ArrayList<>();
		assignments.forEach((id, a) -> {
			int completed = 0;
			List<BigDecimal> graded = new ArrayList<>();
			for (StudentRef m : members) {
				Key k = new Key(m.id(), id);
				if (a.partCount() > 0 && submitted.getOrDefault(k, new int[2])[0] == a.partCount()) {
					completed++;
				}
				if (bands.containsKey(k)) {
					graded.add(bands.get(k));
				}
			}
			assignmentStats.add(new ClassAssignment(id, a.title(), a.kind(), a.status(), a.dueAt(), a.partCount(), completed,
					members.isEmpty() ? 0 : (double) completed / members.size(), graded.size(),
					StatsMath.average(graded)));
		});

		List<RankingRow> ranking = new ArrayList<>();
		for (StudentRef m : members) {
			List<BigDecimal> own = new ArrayList<>();
			Map<Long, BigDecimal> assignmentBands = new LinkedHashMap<>();
			int submittedParts = 0;
			int lateParts = 0;
			for (Long id : assignments.keySet()) {
				Key k = new Key(m.id(), id);
				int[] counts = submitted.getOrDefault(k, new int[2]);
				submittedParts += counts[0];
				lateParts += counts[1];
				if (bands.containsKey(k)) {
					BigDecimal band = bands.get(k);
					// Bài PAPER thang 10 chỉ hiện trong bảng điểm, không gộp vào band trung bình / xếp hạng IELTS.
					if (assignments.get(id).kind() != AssignmentKind.PAPER) {
						own.add(band);
					}
					assignmentBands.put(id, StatsMath.round(band));
				} else {
					assignmentBands.put(id, null);
				}
			}
			ranking.add(new RankingRow(null, m, StatsMath.average(own), own.size(), submittedParts, lateParts,
					assignmentBands));
		}
		List<BigDecimal> essayBands = bands.entrySet().stream()
				.filter(e -> assignments.get(e.getKey().assignmentId()).kind() != AssignmentKind.PAPER)
				.map(Map.Entry::getValue).toList();
		return new ClassStats(new ClassInfo(c.id(), c.code(), c.name(), c.memberCount()),
				StatsMath.average(essayBands), assignmentStats, rank(ranking));
	}

	/** Band cao trước; bằng band thì cùng hạng (1, 1, 3); chưa có band → không xếp hạng, đứng cuối theo mã SV. */
	static List<RankingRow> rank(List<RankingRow> rows) {
		List<RankingRow> sorted = new ArrayList<>(rows);
		sorted.sort(Comparator.comparing(RankingRow::averageBand, Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(r -> r.student().studentCode()));
		List<RankingRow> out = new ArrayList<>();
		for (int i = 0; i < sorted.size(); i++) {
			RankingRow r = sorted.get(i);
			Integer rank = null;
			if (r.averageBand() != null) {
				RankingRow prev = i == 0 ? null : out.get(i - 1);
				rank = prev != null && r.averageBand().compareTo(prev.averageBand()) == 0 ? prev.rank() : i + 1;
			}
			out.add(new RankingRow(rank, r.student(), r.averageBand(), r.gradedAssignments(), r.submittedParts(),
					r.lateParts(), r.assignmentBands()));
		}
		return out;
	}

	private record Key(long studentId, long assignmentId) {
	}

	private record AssignmentRow(String title, AssignmentStatus status, Instant dueAt, int partCount,
			AssignmentKind kind) {
	}
}
