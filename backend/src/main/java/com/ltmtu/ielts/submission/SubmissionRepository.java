package com.ltmtu.ielts.submission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ltmtu.ielts.submission.dto.TeacherSubmissionDtos.QueueItem;

import jakarta.persistence.LockModeType;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

	/** Bài làm cá nhân (bài luận) — bài PAPER thuộc nhóm, tìm bằng {@link #findMine} / {@link #findByPartIdAndGroupId}. */
	@Query("SELECT s FROM Submission s WHERE s.part.id = :partId AND s.student.id = :studentId AND s.groupId IS NULL")
	Optional<Submission> findByPartIdAndStudentId(@Param("partId") Long partId, @Param("studentId") Long studentId);

	/**
	 * {@code SELECT … FOR UPDATE} bài làm cá nhân: hai request nộp cùng lúc cho cùng một phần phải xếp hàng (SD-07).
	 * Bài PAPER: {@link #findForUpdateByGroup}.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Submission s WHERE s.part.id = :partId AND s.student.id = :studentId AND s.groupId IS NULL")
	Optional<Submission> findForUpdate(@Param("partId") Long partId, @Param("studentId") Long studentId);

	/**
	 * Sinh viên đã bắt đầu phiên thi của phần chưa (UC-27). Truy vấn chỉ-đọc bảng của module {@code exam} — module đó
	 * nằm trên module bài nộp nên không gọi service của nó được (AI-CODING-RULES mục 3).
	 */
	@Query(value = "SELECT EXISTS (SELECT 1 FROM exam_sessions WHERE part_id = :partId AND student_id = :studentId)",
			nativeQuery = true)
	boolean examStarted(@Param("partId") Long partId, @Param("studentId") Long studentId);

	// ─── Bài PAPER: bài làm thuộc nhóm (BR-28, V17) ─────────────────────────

	/** Nhóm của sinh viên trong một bài (chỉ đọc bảng của module paper). */
	@Query(nativeQuery = true,
			value = "SELECT group_id FROM paper_group_members WHERE assignment_id = :assignmentId AND student_id = :studentId")
	Optional<Long> groupOf(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId);

	Optional<Submission> findByPartIdAndGroupId(Long partId, Long groupId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Submission s WHERE s.part.id = :partId AND s.groupId = :groupId")
	Optional<Submission> findForUpdateByGroup(@Param("partId") Long partId, @Param("groupId") Long groupId);

	List<Submission> findByGroupIdAndPartIdIn(Long groupId, Collection<Long> partIds);

	/**
	 * Bài làm "của tôi" cho một phần — bài luận của chính mình, hoặc bài của nhóm mình (bài PAPER). Đọc qua view
	 * {@code submission_members} nên đúng cho cả hai loại bài.
	 */
	@Query(nativeQuery = true, value = """
			SELECT s.* FROM submissions s JOIN submission_members sm ON sm.submission_id = s.id
			WHERE sm.student_id = :studentId AND s.part_id = :partId""")
	Optional<Submission> findMine(@Param("studentId") Long studentId, @Param("partId") Long partId);

	/** Như {@link #findMine}, cho nhiều phần một lúc (tránh N+1). */
	@Query(nativeQuery = true, value = """
			SELECT s.* FROM submissions s JOIN submission_members sm ON sm.submission_id = s.id
			WHERE sm.student_id = :studentId AND s.part_id IN (:partIds)""")
	List<Submission> findMineInParts(@Param("studentId") Long studentId, @Param("partIds") Collection<Long> partIds);

	/** Những sinh viên thuộc bài làm: bài luận → 1 người, bài PAPER → thành viên hiện tại của nhóm. */
	@Query(nativeQuery = true, value = "SELECT student_id FROM submission_members WHERE submission_id = :id")
	List<Long> memberIds(@Param("id") Long submissionId);

	/** Bài nộp kèm phần, bài tập, sinh viên — cho màn chấm. */
	@Query("SELECT s FROM Submission s JOIN FETCH s.part p JOIN FETCH p.assignment JOIN FETCH s.student WHERE s.id = :id")
	Optional<Submission> findWithPart(@Param("id") Long id);

	/** Khoá dòng bài nộp khi lưu chấm / trả bài: tránh đua với lần lưu khác hoặc với sinh viên nộp lại. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Submission s WHERE s.id = :id")
	Optional<Submission> findByIdForUpdate(@Param("id") Long id);

	/**
	 * Hàng đợi chấm (UC-17): bài nộp của các bài tập do giáo viên tạo, ở một trạng thái, kèm version mới nhất.
	 * Sắp theo thời điểm nộp — bài nộp trước được chấm trước.
	 */
	@Query(value = """
			SELECT new com.ltmtu.ielts.submission.dto.TeacherSubmissionDtos$QueueItem(
				s.id, s.status, s.currentVersion, u.id, u.studentCode, u.fullName, a.id, a.title, p.id, p.title,
				p.orderNo, p.taskType, v.id, v.submittedAt, v.late, v.wordCount)
			FROM SubmissionVersion v JOIN v.submission s JOIN s.part p JOIN p.assignment a JOIN s.student u
			WHERE v.versionNo = s.currentVersion AND a.teacher.id = :teacherId AND s.status = :status
			  AND (:assignmentId IS NULL OR a.id = :assignmentId)
			  AND (:partId IS NULL OR p.id = :partId)
			  AND (:classId IS NULL OR EXISTS (
			      SELECT 1 FROM ClassMember m WHERE m.classroom.id = :classId AND m.student = u))
			  AND (:keyword IS NULL OR lower(u.studentCode) LIKE :keyword ESCAPE '!'
			       OR lower(u.fullName) LIKE :keyword ESCAPE '!')
			ORDER BY v.submittedAt ASC, s.id""",
			countQuery = """
					SELECT count(s) FROM Submission s JOIN s.part p JOIN p.assignment a JOIN s.student u
					WHERE s.currentVersion > 0 AND a.teacher.id = :teacherId AND s.status = :status
					  AND (:assignmentId IS NULL OR a.id = :assignmentId)
					  AND (:partId IS NULL OR p.id = :partId)
					  AND (:classId IS NULL OR EXISTS (
					      SELECT 1 FROM ClassMember m WHERE m.classroom.id = :classId AND m.student = u))
					  AND (:keyword IS NULL OR lower(u.studentCode) LIKE :keyword ESCAPE '!'
					       OR lower(u.fullName) LIKE :keyword ESCAPE '!')""")
	Page<QueueItem> gradingQueue(@Param("teacherId") Long teacherId, @Param("status") SubmissionStatus status,
			@Param("assignmentId") Long assignmentId, @Param("partId") Long partId, @Param("classId") Long classId,
			@Param("keyword") String keyword, Pageable pageable);
}
