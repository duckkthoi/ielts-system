package com.ltmtu.ielts.paper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaperGroupMemberRepository extends JpaRepository<PaperGroupMember, PaperGroupMember.Id> {

	/** Mọi thành viên của các nhóm trong bài, kèm sinh viên (một câu, tránh N+1). */
	@Query("""
			SELECT m FROM PaperGroupMember m JOIN FETCH m.student
			WHERE m.assignmentId = :assignmentId ORDER BY m.joinedAt, m.id.studentId""")
	List<PaperGroupMember> findByAssignment(@Param("assignmentId") Long assignmentId);

	Optional<PaperGroupMember> findByAssignmentIdAndIdStudentId(Long assignmentId, Long studentId);

	long countByIdGroupId(Long groupId);

	/** Xoá hàng loạt rồi xoá luôn bộ nhớ đệm của JPA, để thêm lại cùng thành viên ngay sau đó không bị lỗi. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM PaperGroupMember m WHERE m.assignmentId = :assignmentId AND m.id.studentId IN :studentIds")
	int deleteByAssignmentAndStudents(@Param("assignmentId") Long assignmentId,
			@Param("studentIds") Collection<Long> studentIds);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM PaperGroupMember m WHERE m.id.groupId = :groupId")
	int deleteByGroup(@Param("groupId") Long groupId);

	/** Thành viên hiện tại của một lớp (chỉ đọc bảng của module lớp học) — chia nhóm ngẫu nhiên theo lớp. */
	@Query(nativeQuery = true, value = "SELECT student_id FROM class_members WHERE classroom_id = :classId")
	Set<Long> classMemberIds(@Param("classId") Long classId);
}
