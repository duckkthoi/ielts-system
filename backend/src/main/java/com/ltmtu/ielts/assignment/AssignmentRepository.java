package com.ltmtu.ielts.assignment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentRow;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

	/** Số thành viên của nhóm đông nhất trong bài (chỉ đọc bảng nhóm của module paper). */
	@Query(nativeQuery = true, value = """
			SELECT COALESCE(MAX(c), 0) FROM (
			    SELECT count(*) AS c FROM paper_group_members WHERE assignment_id = :id GROUP BY group_id) g""")
	int largestGroupSize(@Param("id") Long assignmentId);

	/** Bài tập chỉ "tồn tại" với giáo viên tạo ra nó (BR-15). */
	Optional<Assignment> findByIdAndTeacherId(Long id, Long teacherId);

	/**
	 * Điều kiện "sinh viên được giao và được thấy bài" — dùng chung ở mọi nơi phía sinh viên:
	 * <ul>
	 * <li>BR-05: bài đã phát hành hoặc đã đóng (vẫn xem được kết quả) và đã tới thời điểm mở;</li>
	 * <li>BR-04: được giao trực tiếp, HOẶC giao cho một lớp mà sinh viên đang là thành viên.</li>
	 * </ul>
	 */
	String VISIBLE_TO_STUDENT = """
			a.status IN (PUBLISHED, CLOSED)
			AND (a.openAt IS NULL OR a.openAt <= :now)
			AND EXISTS (
			  SELECT 1 FROM AssignmentTarget t
			  WHERE t.assignment = a
			    AND (t.student.id = :studentId
			         OR EXISTS (SELECT 1 FROM ClassMember m WHERE m.classroom = t.classroom AND m.student.id = :studentId)))""";

	@Query("SELECT a FROM Assignment a WHERE " + VISIBLE_TO_STUDENT + " ORDER BY a.dueAt ASC NULLS LAST, a.id")
	List<Assignment> findVisibleForStudent(@Param("studentId") Long studentId, @Param("now") Instant now);

	@Query("SELECT a FROM Assignment a WHERE a.id = :id AND " + VISIBLE_TO_STUDENT)
	Optional<Assignment> findVisibleForStudent(@Param("id") Long id, @Param("studentId") Long studentId,
			@Param("now") Instant now);

	@Query(value = """
			SELECT new com.ltmtu.ielts.assignment.dto.AssignmentDtos$AssignmentRow(
				a.id, a.title, a.status, a.openAt, a.dueAt, size(a.parts), a.publishedAt, a.createdAt, a.updatedAt)
			FROM Assignment a
			WHERE a.teacher.id = :teacherId
			  AND (:status IS NULL OR a.status = :status)
			  AND (:keyword IS NULL OR lower(a.title) LIKE :keyword ESCAPE '!')""",
			countQuery = """
					SELECT count(a) FROM Assignment a
					WHERE a.teacher.id = :teacherId
					  AND (:status IS NULL OR a.status = :status)
					  AND (:keyword IS NULL OR lower(a.title) LIKE :keyword ESCAPE '!')""")
	Page<AssignmentRow> searchOfTeacher(@Param("teacherId") Long teacherId,
			@Param("status") AssignmentStatus status, @Param("keyword") String keyword, Pageable pageable);

	/**
	 * Bài đã phát hành TRƯỚC giờ mở, nay đã tới giờ mở mà chưa thông báo. Bài phát hành khi đã mở thì đã được thông báo
	 * ngay lúc phát hành ({@code open_at <= published_at}), nên không lọt vào đây.
	 */
	@Query("SELECT a FROM Assignment a WHERE a.status = PUBLISHED AND a.openNotifiedAt IS NULL "
			+ "AND a.openAt IS NOT NULL AND a.openAt <= :now AND a.openAt > a.publishedAt ORDER BY a.openAt")
	List<Assignment> findOpenedNotAnnounced(@Param("now") Instant now);

	/** "Giành" quyền thông báo cho một bài: chỉ một lần chạy job (kể cả chạy chồng nhau) cập nhật được dòng này. */
	@Modifying(flushAutomatically = true)
	@Query("UPDATE Assignment a SET a.openNotifiedAt = :now WHERE a.id = :id AND a.openNotifiedAt IS NULL")
	int claimOpenNotification(@Param("id") Long id, @Param("now") Instant now);

	/** Bài PAPER đang mở đã tới hạn chốt nhóm (không đặt = hạn nộp) mà chưa báo giáo viên (BR-27). */
	@Query("""
			SELECT a FROM Assignment a JOIN FETCH a.teacher WHERE a.status = PUBLISHED
			  AND a.kind = com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentKind.PAPER
			  AND a.groupLockNotifiedAt IS NULL AND COALESCE(a.groupLockAt, a.dueAt) <= :now""")
	List<Assignment> findGroupLockDue(@Param("now") Instant now);

	@Modifying(flushAutomatically = true)
	@Query("UPDATE Assignment a SET a.groupLockNotifiedAt = :now WHERE a.id = :id AND a.groupLockNotifiedAt IS NULL")
	int claimGroupLockNotification(@Param("id") Long id, @Param("now") Instant now);

	/** Số liệu nộp bài của một bài tập trong danh sách. */
	interface ProgressCounts {
		Long getAssignmentId();

		long getRecipients();

		long getCompleted();

		long getPending();
	}

	/**
	 * Số liệu cho một trang danh sách, gộp trong một câu SQL. Người nhận tính như {@code findRecipientIds} (giao
	 * trực tiếp ∪ thành viên lớp, tại thời điểm truy vấn). "Đã nộp đủ" = người nhận có bài nộp (đã nộp ít nhất một
	 * lần) cho mọi phần. Native SQL vì JPQL không có UNION / CTE.
	 */
	@Query(nativeQuery = true, value = """
			WITH r AS (
			    SELECT t.assignment_id, t.student_id FROM assignment_targets t
			    WHERE t.assignment_id IN (:ids) AND t.student_id IS NOT NULL
			    UNION
			    SELECT t.assignment_id, m.student_id FROM assignment_targets t
			    JOIN class_members m ON m.classroom_id = t.classroom_id
			    WHERE t.assignment_id IN (:ids)
			), done AS (
			    SELECT p.assignment_id, sm.student_id, count(*) AS parts_done
			    FROM submissions s JOIN submission_members sm ON sm.submission_id = s.id
			    JOIN assignment_parts p ON p.id = s.part_id
			    WHERE p.assignment_id IN (:ids) AND s.current_version > 0
			    GROUP BY p.assignment_id, sm.student_id
			)
			SELECT a.id AS assignmentId,
			       (SELECT count(*) FROM r WHERE r.assignment_id = a.id) AS recipients,
			       (SELECT count(*) FROM done d JOIN r ON r.assignment_id = d.assignment_id AND r.student_id = d.student_id
			        WHERE d.assignment_id = a.id
			          AND d.parts_done = (SELECT count(*) FROM assignment_parts p WHERE p.assignment_id = a.id)) AS completed,
			       (SELECT count(*) FROM submissions s JOIN assignment_parts p ON p.id = s.part_id
			        WHERE p.assignment_id = a.id AND s.status = 'SUBMITTED') AS pending
			FROM assignments a WHERE a.id IN (:ids)""")
	List<ProgressCounts> progressCounts(@Param("ids") Collection<Long> assignmentIds);
}
