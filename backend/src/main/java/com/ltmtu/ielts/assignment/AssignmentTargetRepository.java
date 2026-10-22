package com.ltmtu.ielts.assignment;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentTargetRepository extends JpaRepository<AssignmentTarget, Long> {

	@Query("SELECT t FROM AssignmentTarget t LEFT JOIN FETCH t.classroom LEFT JOIN FETCH t.student "
			+ "WHERE t.assignment.id = :assignmentId ORDER BY t.id")
	List<AssignmentTarget> findWithDetails(@Param("assignmentId") Long assignmentId);

	/**
	 * Danh sách người nhận bài thực tế = sinh viên được giao trực tiếp ∪ thành viên các lớp được giao (đã loại trùng).
	 * Tính tại thời điểm truy vấn, nên phản ánh cả thay đổi thành viên lớp sau khi phát hành.
	 */
	@Query(nativeQuery = true, value = """
			SELECT t.student_id FROM assignment_targets t
			WHERE t.assignment_id = :assignmentId AND t.student_id IS NOT NULL
			UNION
			SELECT m.student_id FROM assignment_targets t
			JOIN class_members m ON m.classroom_id = t.classroom_id
			WHERE t.assignment_id = :assignmentId""")
	Set<Long> findRecipientIds(@Param("assignmentId") Long assignmentId);
}
