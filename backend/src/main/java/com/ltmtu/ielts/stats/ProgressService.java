package com.ltmtu.ielts.stats;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.TeacherAssignmentAccess;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.grading.BandCalculator;
import com.ltmtu.ielts.realtime.PresenceService;
import com.ltmtu.ielts.stats.dto.ProgressDtos.PartColumn;
import com.ltmtu.ielts.stats.dto.ProgressDtos.ProgressCell;
import com.ltmtu.ielts.stats.dto.ProgressDtos.ProgressMatrix;
import com.ltmtu.ielts.stats.dto.ProgressDtos.ProgressRow;
import com.ltmtu.ielts.stats.dto.ProgressDtos.ProgressSummary;
import com.ltmtu.ielts.stats.dto.ProgressDtos.StudentRef;
import com.ltmtu.ielts.submission.DeadlineRules;
import com.ltmtu.ielts.submission.SubmissionStatus;

import lombok.RequiredArgsConstructor;

/**
 * Ma trận tiến độ của một bài tập (UC-16). Toàn bộ ô (sinh viên × phần) lấy bằng <b>một câu SQL</b>, không tải
 * entity bài nộp: số dòng = số sinh viên × số phần, nên tránh N+1.
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

	/** Mỗi sinh viên của bài ({@link StatsSql#PEOPLE}) ghép với mọi phần; ô chưa có bài làm có {@code status = NULL}. */
	private static final String CELLS_SQL = "WITH " + StatsSql.PEOPLE + """

			SELECT u.id AS student_id, u.student_code, u.full_name,
			       p.id AS part_id, p.part_due_at,
			       s.id AS submission_id, s.status, s.current_version, s.revision_due_at,
			       v.is_late, g.overall_score
			FROM people r
			JOIN users u ON u.id = r.student_id
			JOIN assignment_parts p ON p.assignment_id = :assignmentId
			-- Bài làm của sinh viên: của chính mình hoặc của nhóm mình (bài PAPER — view submission_members, V17).
			LEFT JOIN LATERAL (
			    SELECT x.* FROM submissions x JOIN submission_members sm ON sm.submission_id = x.id
			    WHERE x.part_id = p.id AND sm.student_id = u.id LIMIT 1) s ON true
			LEFT JOIN submission_versions v ON v.submission_id = s.id AND v.version_no = s.current_version
			LEFT JOIN grades g ON g.submission_version_id = v.id AND g.status = 'RETURNED'
			ORDER BY u.student_code, u.id, p.order_no""";

	private final TeacherAssignmentAccess assignmentAccess;
	private final ClassroomService classroomService;
	private final NamedParameterJdbcTemplate jdbc;
	private final Clock clock;
	private final PresenceService presence;

	/**
	 * UC-16: ma trận tiến độ. Bài của giáo viên khác → 404; {@code classId} không phải lớp của mình → 400.
	 * {@code status} chỉ giữ các hàng có ít nhất một ô ở trạng thái đó; phần tóm tắt tính trước khi lọc trạng thái.
	 * Giáo viên thấy band của phần khi kết quả lần nộp mới nhất đã trả (cùng điều kiện sinh viên thấy, BR-13).
	 */
	@Transactional(readOnly = true)
	public ProgressMatrix progress(Long teacherId, Long assignmentId, Long classId, SubmissionStatus status) {
		Assignment a = assignmentAccess.requireOwned(teacherId, assignmentId);
		if (classId != null) {
			classroomService.requireOwnedClassrooms(teacherId, List.of(classId));
		}
		List<PartColumn> parts = a.getParts().stream()
				.map(p -> new PartColumn(p.getId(), p.getOrderNo(), p.getTitle(), p.getTaskType())).toList();
		Instant now = clock.instant();

		Map<Long, ProgressRow> rows = new LinkedHashMap<>();
		jdbc.query(CELLS_SQL, StatsSql.params(assignmentId, classId), rs -> {
			long studentId = rs.getLong("student_id");
			ProgressRow row = rows.get(studentId);
			if (row == null) {
				row = new ProgressRow(
						new StudentRef(studentId, rs.getString("student_code"), rs.getString("full_name")),
						presence.isOnline(studentId), presence.viewingPart(studentId, assignmentId), new ArrayList<>());
				rows.put(studentId, row);
			}
			row.cells().add(cell(rs, a, now));
		});

		List<ProgressRow> all = new ArrayList<>(rows.values());
		ProgressSummary summary = summarize(all);
		List<ProgressRow> shown = status == null ? all
				: all.stream().filter(r -> r.cells().stream().anyMatch(c -> c.status() == status)).toList();
		return new ProgressMatrix(parts, shown, summary);
	}

	private static ProgressCell cell(ResultSet rs, Assignment a, Instant now) throws SQLException {
		String rawStatus = rs.getString("status");
		SubmissionStatus status = rawStatus == null ? SubmissionStatus.NOT_STARTED : SubmissionStatus.valueOf(rawStatus);
		Instant due = DeadlineRules.effectiveDue(a.getDueAt(), StatsSql.instant(rs, "part_due_at"), status,
				StatsSql.instant(rs, "revision_due_at"));
		boolean overdue = status.isOpenForStudent() && DeadlineRules.isPastDue(due, now);
		BigDecimal band = rs.getBigDecimal("overall_score");
		return new ProgressCell(rs.getLong("part_id"), rs.getObject("submission_id", Long.class), status,
				rs.getInt("current_version"), rs.getBoolean("is_late"), overdue, BandCalculator.display(band));
	}

	private static ProgressSummary summarize(List<ProgressRow> rows) {
		int completed = 0;
		int late = 0;
		int overdue = 0;
		for (ProgressRow r : rows) {
			if (r.cells().stream().allMatch(c -> c.currentVersion() > 0)) {
				completed++;
			}
			if (r.cells().stream().anyMatch(ProgressCell::late)) {
				late++;
			}
			if (r.cells().stream().anyMatch(ProgressCell::overdue)) {
				overdue++;
			}
		}
		double rate = rows.isEmpty() ? 0 : (double) completed / rows.size();
		return new ProgressSummary(rows.size(), completed, rate, late, overdue);
	}
}
