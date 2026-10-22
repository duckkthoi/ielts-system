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

	Optional<Submission> findByPartIdAndStudentId(Long partId, Long studentId);

	/** {@code SELECT … FOR UPDATE}: hai request nộp cùng lúc cho cùng một phần phải xếp hàng (SD-07). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Submission s WHERE s.part.id = :partId AND s.student.id = :studentId")
	Optional<Submission> findForUpdate(@Param("partId") Long partId, @Param("studentId") Long studentId);

	List<Submission> findByStudentIdAndPartIdIn(Long studentId, Collection<Long> partIds);

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
