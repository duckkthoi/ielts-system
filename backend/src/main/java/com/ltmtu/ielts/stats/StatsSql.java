package com.ltmtu.ielts.stats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import com.ltmtu.ielts.stats.dto.AssignmentStatsDtos.ErrorCount;

/**
 * Các đoạn SQL dùng chung cho tiến độ / thống kê, để mọi màn hình tính trên <b>cùng một tập sinh viên và cùng một
 * kết quả chấm</b>. Truy vấn theo một bài dùng tham số {@code :assignmentId}, {@code :classId} (có thể null).
 */
final class StatsSql {

	private StatsSql() {
	}

	/**
	 * {@code people(student_id)}: người nhận bài (giao trực tiếp ∪ thành viên lớp được giao, tính tại thời điểm truy
	 * vấn như {@code findRecipientIds}) ∪ người đã có bài làm — để bài đã nộp không "biến mất" khi sinh viên rời lớp.
	 * Có {@code classId} thì chỉ giữ thành viên hiện tại của lớp đó.
	 */
	static final String PEOPLE = """
			people AS (
			    SELECT x.student_id FROM (
			        SELECT t.student_id FROM assignment_targets t
			        WHERE t.assignment_id = :assignmentId AND t.student_id IS NOT NULL
			        UNION
			        SELECT m.student_id FROM assignment_targets t
			        JOIN class_members m ON m.classroom_id = t.classroom_id
			        WHERE t.assignment_id = :assignmentId
			        UNION
			        SELECT s.student_id FROM submissions s JOIN assignment_parts p ON p.id = s.part_id
			        WHERE p.assignment_id = :assignmentId
			    ) x
			    WHERE CAST(:classId AS BIGINT) IS NULL
			       OR EXISTS (SELECT 1 FROM class_members cm WHERE cm.classroom_id = :classId AND cm.student_id = x.student_id)
			)""";

	/**
	 * CTE {@code latest(submission_id, student_id, assignment_id, part_id, grade_id, grader_id, overall_score,
	 * returned_at)}: với mỗi bài làm thoả {@code where} (bí danh {@code s} = submissions, {@code p} = assignment_parts),
	 * kết quả <b>đã trả</b> của version mới nhất có kết quả đã trả (BR-13) — cùng cách chọn với màn kết quả của sinh
	 * viên ({@code StudentResultService}).
	 */
	static String latestReturned(String where) {
		return """
				latest AS (
				    SELECT DISTINCT ON (s.id) s.id AS submission_id, s.student_id, p.assignment_id, s.part_id,
				           g.id AS grade_id, g.grader_id, g.overall_score, g.returned_at
				    FROM submissions s
				    JOIN assignment_parts p ON p.id = s.part_id
				    JOIN submission_versions v ON v.submission_id = s.id
				    JOIN grades g ON g.submission_version_id = v.id AND g.status = 'RETURNED'
				    WHERE %s
				    ORDER BY s.id, v.version_no DESC
				)""".formatted(where);
	}

	/** {@link #latestReturned} cho các sinh viên của một bài ({@link #PEOPLE} phải đứng trước). */
	static final String LATEST_RETURNED = latestReturned(
			"p.assignment_id = :assignmentId AND s.student_id IN (SELECT student_id FROM people)");

	/**
	 * Mã lỗi hay gặp trong nhận xét inline của các kết quả {@code latest}: số lần, số sinh viên. Tên / nhóm tiêu chí
	 * tra theo bộ mã của người chấm — mã hệ thống trước, rồi mã riêng (mã riêng không trùng mã hệ thống, BR-22). Bỏ
	 * mã khen ({@code positive}). Tham số {@code :limit}.
	 */
	static final String TOP_ERRORS = """
			SELECT ic.error_code AS code, e.name, e.criterion,
			       count(*) AS total, count(DISTINCT l.student_id) AS students
			FROM latest l
			JOIN inline_comments ic ON ic.grade_id = l.grade_id
			LEFT JOIN LATERAL (
			    SELECT ec.name, ec.criterion, ec.positive FROM error_codes ec
			    WHERE upper(ec.code) = upper(ic.error_code) AND (ec.owner_id IS NULL OR ec.owner_id = l.grader_id)
			    ORDER BY ec.owner_id NULLS FIRST LIMIT 1
			) e ON TRUE
			WHERE ic.error_code IS NOT NULL AND NOT coalesce(e.positive, FALSE)
			GROUP BY ic.error_code, e.name, e.criterion
			ORDER BY total DESC, students DESC, ic.error_code
			LIMIT :limit""";

	/** Số mã lỗi hay gặp trả về. */
	static final int TOP_ERROR_LIMIT = 10;

	static final RowMapper<ErrorCount> ERROR_COUNT = (rs, i) -> new ErrorCount(rs.getString("code"),
			rs.getString("name"), rs.getString("criterion"), rs.getInt("total"), rs.getInt("students"));

	static MapSqlParameterSource params(Long assignmentId, Long classId) {
		return new MapSqlParameterSource("assignmentId", assignmentId).addValue("classId", classId, Types.BIGINT);
	}

	/** Cột {@code timestamptz} → {@link Instant} (null giữ null). */
	static Instant instant(ResultSet rs, String column) throws SQLException {
		Timestamp t = rs.getTimestamp(column);
		return t == null ? null : t.toInstant();
	}
}
