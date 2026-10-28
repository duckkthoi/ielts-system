package com.ltmtu.ielts.exam;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

	Optional<ExamSession> findByPartIdAndStudentId(Long partId, Long studentId);

	/**
	 * Tạo phiên thi nếu chưa có (BR-23). Hai lần bấm "Bắt đầu" đồng thời (bấm đúp, 2 tab) chỉ tạo đúng một phiên:
	 * UNIQUE {@code (part_id, student_id)} + {@code ON CONFLICT DO NOTHING}. Trả 1 nếu vừa tạo, 0 nếu đã có.
	 */
	@Modifying
	@Query(nativeQuery = true, value = """
			INSERT INTO exam_sessions (part_id, student_id, started_at, ends_at)
			VALUES (:partId, :studentId, :startedAt, :endsAt)
			ON CONFLICT (part_id, student_id) DO NOTHING""")
	int insertIfAbsent(@Param("partId") Long partId, @Param("studentId") Long studentId,
			@Param("startedAt") Instant startedAt, @Param("endsAt") Instant endsAt);

	/** Phiên kèm phần, bài tập, giáo viên, sinh viên — đủ dữ liệu để ghi sự kiện, đẩy tin và tạo thông báo. */
	@Query("""
			SELECT s FROM ExamSession s JOIN FETCH s.part p JOIN FETCH p.assignment a JOIN FETCH a.teacher
			JOIN FETCH s.student WHERE s.id = :id""")
	Optional<ExamSession> findFull(@Param("id") Long id);

	/** Như {@link #findFull}, tìm theo phần + sinh viên. */
	@Query("""
			SELECT s FROM ExamSession s JOIN FETCH s.part p JOIN FETCH p.assignment a JOIN FETCH a.teacher
			JOIN FETCH s.student WHERE p.id = :partId AND s.student.id = :studentId""")
	Optional<ExamSession> findFull(@Param("partId") Long partId, @Param("studentId") Long studentId);

	/** Phiên chưa nộp của một sinh viên (kể cả đã quá giờ) — để ghi mất kết nối / kết nối lại. */
	@Query("""
			SELECT s FROM ExamSession s JOIN FETCH s.part p JOIN FETCH p.assignment a JOIN FETCH a.teacher
			JOIN FETCH s.student WHERE s.student.id = :studentId AND s.finishedAt IS NULL""")
	List<ExamSession> findUnfinished(@Param("studentId") Long studentId);

	/** Phiên đã hết giờ, chưa nộp, chưa báo quá giờ (job UC-31). */
	@Query("""
			SELECT s.id FROM ExamSession s
			WHERE s.finishedAt IS NULL AND s.overtimeNotifiedAt IS NULL AND s.endsAt <= :now ORDER BY s.endsAt""")
	List<Long> findOverdueIds(@Param("now") Instant now);

	/** Nhận việc báo quá giờ cho một phiên: lượt job chạy chồng nhau chỉ một lượt nhận được (trả 1). */
	@Modifying
	@Query("""
			UPDATE ExamSession s SET s.overtimeNotifiedAt = :now
			WHERE s.id = :id AND s.overtimeNotifiedAt IS NULL AND s.finishedAt IS NULL""")
	int claimOvertime(@Param("id") Long id, @Param("now") Instant now);

	/**
	 * Các phiên thi của một bài cho màn giám sát, mới bắt đầu trước. Có {@code classId} thì chỉ thành viên hiện tại
	 * của lớp (đọc bảng của module lớp học, chỉ đọc).
	 */
	@Query("""
			SELECT s FROM ExamSession s JOIN FETCH s.part p JOIN FETCH s.student u
			WHERE p.assignment.id = :assignmentId
			  AND (:classId IS NULL OR EXISTS (
			      SELECT 1 FROM ClassMember m WHERE m.classroom.id = :classId AND m.student = u))
			ORDER BY s.startedAt DESC, s.id DESC""")
	List<ExamSession> findForMonitor(@Param("assignmentId") Long assignmentId, @Param("classId") Long classId);
}
