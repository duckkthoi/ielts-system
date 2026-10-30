package com.ltmtu.ielts.exam;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ltmtu.ielts.exam.ExamEnums.ExamEventType;

public interface ExamEventRepository extends JpaRepository<ExamEvent, Long> {

	/** Số sự kiện mỗi loại của từng phiên, và lần gần nhất. */
	interface TypeCount {
		Long getSessionId();

		ExamEventType getType();

		long getTotal();

		Instant getLastAt();
	}

	List<ExamEvent> findBySessionIdOrderByOccurredAtAscIdAsc(Long sessionId);

	/**
	 * Sự kiện mới nhất của mọi phiên thi trong một bài (bảng tin của màn giám sát). Có {@code classId} thì chỉ thành viên
	 * hiện tại của lớp (đọc bảng của module lớp học, chỉ đọc).
	 */
	@Query("""
			SELECT e FROM ExamEvent e JOIN FETCH e.session s JOIN FETCH s.part p JOIN FETCH s.student u
			WHERE p.assignment.id = :assignmentId
			  AND (:classId IS NULL OR EXISTS (
			      SELECT 1 FROM ClassMember m WHERE m.classroom.id = :classId AND m.student = u))
			ORDER BY e.occurredAt DESC, e.id DESC""")
	List<ExamEvent> findRecent(@Param("assignmentId") Long assignmentId, @Param("classId") Long classId,
			Pageable page);

	@Query("""
			SELECT e.session.id AS sessionId, e.type AS type, count(e) AS total, max(e.occurredAt) AS lastAt
			FROM ExamEvent e WHERE e.session.id IN :sessionIds GROUP BY e.session.id, e.type""")
	List<TypeCount> countByType(@Param("sessionIds") Collection<Long> sessionIds);
}
