package com.ltmtu.ielts.realtime;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.StudentAssignmentAccess;
import com.ltmtu.ielts.assignment.TeacherAssignmentAccess;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.realtime.dto.StudentActivityDtos.StudentActivityEvent;
import com.ltmtu.ielts.realtime.dto.StudentActivityDtos.RecordActivityRequest;
import com.ltmtu.ielts.user.User;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;

/** Ghi nhận hoạt động màn làm bài và cung cấp lịch sử cho giáo viên (FR-54). */
@Service
@RequiredArgsConstructor
public class StudentActivityService {

	private static final String FEED_SQL = """
			SELECT e.id, e.student_id, u.student_code, u.full_name, e.event_type, e.occurred_at
			FROM student_activity_events e
			JOIN users u ON u.id = e.student_id
			WHERE e.assignment_id = :assignmentId
			  AND (CAST(:classId AS BIGINT) IS NULL OR EXISTS (
			      SELECT 1 FROM class_members cm
			      WHERE cm.classroom_id = :classId AND cm.student_id = e.student_id
			  ))
			ORDER BY e.occurred_at DESC, e.id DESC
			LIMIT :limit
			""";

	private final StudentAssignmentAccess studentAssignmentAccess;
	private final TeacherAssignmentAccess teacherAssignmentAccess;
	private final ClassroomService classroomService;
	private final UserQueryService userQueryService;
	private final NamedParameterJdbcTemplate jdbc;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	/** Lưu một chuyển trạng thái quan sát được khi sinh viên đang ở màn làm bài. */
	@Transactional
	public StudentActivityEvent record(Long studentId, Long assignmentId, RecordActivityRequest request) {
		if (request == null || request.type() == null) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Thiếu loại hoạt động cần ghi nhận");
		}
		studentAssignmentAccess.requireVisibleAssignment(assignmentId, studentId, clock.instant());
		Instant occurredAt = clock.instant();
		Long id = jdbc.queryForObject("""
				INSERT INTO student_activity_events (assignment_id, student_id, event_type, occurred_at)
				VALUES (:assignmentId, :studentId, :type, :occurredAt)
				RETURNING id
				""", Map.of("assignmentId", assignmentId, "studentId", studentId,
					"type", request.type().name(), "occurredAt", Timestamp.from(occurredAt)), Long.class);

		User student = userQueryService.findAllByIds(List.of(studentId)).getFirst();
		StudentActivityEvent activity = new StudentActivityEvent(id, studentId, student.getStudentCode(),
				student.getFullName(), request.type(), occurredAt);
		events.publishEvent(new StudentActivityRecorded(assignmentId, activity));
		return activity;
	}

	/** Giáo viên chỉ xem hoạt động của bài mình sở hữu; {@code classId} lọc theo thành viên lớp hiện tại. */
	@Transactional(readOnly = true)
	public List<StudentActivityEvent> teacherFeed(Long teacherId, Long assignmentId, Long classId, int limit) {
		teacherAssignmentAccess.requireOwned(teacherId, assignmentId);
		if (classId != null) {
			classroomService.requireOwnedClassrooms(teacherId, List.of(classId));
		}
		if (limit < 1 || limit > 200) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Số hoạt động cần xem phải từ 1 đến 200");
		}
		MapSqlParameterSource params = new MapSqlParameterSource("assignmentId", assignmentId)
				.addValue("classId", classId, Types.BIGINT).addValue("limit", limit);
		return jdbc.query(FEED_SQL, params, (rs, row) -> new StudentActivityEvent(rs.getLong("id"),
				rs.getLong("student_id"), rs.getString("student_code"), rs.getString("full_name"),
				StudentActivityType.valueOf(rs.getString("event_type")), rs.getTimestamp("occurred_at").toInstant()));
	}
}
