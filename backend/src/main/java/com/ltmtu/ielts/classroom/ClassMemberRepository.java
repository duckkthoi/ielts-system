package com.ltmtu.ielts.classroom;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ltmtu.ielts.classroom.dto.ClassroomDtos.MemberDto;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.StudentClassroomSummary;

public interface ClassMemberRepository extends JpaRepository<ClassMember, ClassMember.Id> {

	long countByClassroomId(Long classroomId);

	@Query(value = """
			SELECT new com.ltmtu.ielts.classroom.dto.ClassroomDtos$MemberDto(
				u.id, u.studentCode, u.fullName, u.email, u.active, u.mustChangePassword, m.joinedAt)
			FROM ClassMember m JOIN m.student u
			WHERE m.classroom.id = :classroomId
			  AND (:keyword IS NULL OR lower(u.fullName) LIKE :keyword ESCAPE '!' OR lower(u.studentCode) LIKE :keyword ESCAPE '!')""",
			countQuery = """
					SELECT count(m) FROM ClassMember m JOIN m.student u
					WHERE m.classroom.id = :classroomId
					  AND (:keyword IS NULL OR lower(u.fullName) LIKE :keyword ESCAPE '!' OR lower(u.studentCode) LIKE :keyword ESCAPE '!')""")
	Page<MemberDto> searchMembers(@Param("classroomId") Long classroomId, @Param("keyword") String keyword,
			Pageable pageable);

	@Query("SELECT m.student.id FROM ClassMember m WHERE m.classroom.id = :classroomId AND m.student.id IN :studentIds")
	Set<Long> findMemberIds(@Param("classroomId") Long classroomId, @Param("studentIds") Collection<Long> studentIds);

	@Query("""
			SELECT new com.ltmtu.ielts.classroom.dto.ClassroomDtos$StudentClassroomSummary(
				c.id, c.code, c.name, c.description, c.teacher.fullName,
				(SELECT count(DISTINCT t.assignment.id) FROM AssignmentTarget t
				 WHERE t.classroom = c AND t.assignment.status = PUBLISHED), m.joinedAt)
			FROM ClassMember m JOIN m.classroom c
			WHERE m.student.id = :studentId
			ORDER BY c.name, c.id""")
	List<StudentClassroomSummary> findClassesOfStudent(@Param("studentId") Long studentId);
}
