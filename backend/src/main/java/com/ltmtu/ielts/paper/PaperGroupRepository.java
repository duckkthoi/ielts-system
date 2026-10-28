package com.ltmtu.ielts.paper;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PaperGroupRepository extends JpaRepository<PaperGroup, Long> {

	@Query("SELECT g FROM PaperGroup g WHERE g.assignment.id = :assignmentId ORDER BY g.id")
	List<PaperGroup> findByAssignment(@Param("assignmentId") Long assignmentId);

	/** Nhóm của đúng bài này (nhóm của bài khác → rỗng, để trả 404). */
	@Query("SELECT g FROM PaperGroup g JOIN FETCH g.assignment a JOIN FETCH a.teacher WHERE g.id = :id AND a.id = :assignmentId")
	Optional<PaperGroup> findInAssignment(@Param("id") Long id, @Param("assignmentId") Long assignmentId);

	/** Khoá dòng nhóm khi thêm người: hai sinh viên vào chỗ cuối cùng cùng lúc thì xếp hàng (BR-27). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT g FROM PaperGroup g WHERE g.id = :id")
	Optional<PaperGroup> findForUpdate(@Param("id") Long id);

	boolean existsByAssignmentIdAndName(Long assignmentId, String name);

	/**
	 * Nhóm đã có bài làm chưa: bài nộp (chỉ đọc bảng của module bài nộp) hoặc tài liệu đã có chỉnh sửa — có rồi thì không
	 * xoá nhóm / không tự rời nhóm.
	 */
	@Query(nativeQuery = true, value = """
			SELECT EXISTS (SELECT 1 FROM submissions WHERE group_id = :id)
			    OR EXISTS (SELECT 1 FROM paper_documents WHERE group_id = :id AND last_seq > 0)""")
	boolean hasWork(@Param("id") Long groupId);
}
