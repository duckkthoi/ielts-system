package com.ltmtu.ielts.classroom;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ltmtu.ielts.classroom.dto.ClassroomDtos.ClassroomSummary;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

	/** Lớp chỉ "tồn tại" với đúng giáo viên phụ trách (BR-15) — lớp của người khác coi như không có. */
	Optional<Classroom> findByIdAndTeacherId(Long id, Long teacherId);

	Optional<Classroom> findByCode(String code);

	boolean existsByCode(String code);

	@Query(value = """
			SELECT new com.ltmtu.ielts.classroom.dto.ClassroomDtos$ClassroomSummary(
				c.id, c.code, c.name, c.description,
				(SELECT count(m) FROM ClassMember m WHERE m.classroom = c),
				(SELECT count(DISTINCT t.assignment.id) FROM AssignmentTarget t
				 WHERE t.classroom = c AND t.assignment.status = PUBLISHED),
				c.createdAt)
			FROM Classroom c
			WHERE c.teacher.id = :teacherId
			  AND (:keyword IS NULL OR lower(c.name) LIKE :keyword ESCAPE '!' OR lower(c.code) LIKE :keyword ESCAPE '!')""",
			countQuery = """
					SELECT count(c) FROM Classroom c
					WHERE c.teacher.id = :teacherId
					  AND (:keyword IS NULL OR lower(c.name) LIKE :keyword ESCAPE '!' OR lower(c.code) LIKE :keyword ESCAPE '!')""")
	Page<ClassroomSummary> searchOfTeacher(@Param("teacherId") Long teacherId, @Param("keyword") String keyword,
			Pageable pageable);

	@Query("""
			SELECT count(DISTINCT t.assignment.id) FROM AssignmentTarget t
			WHERE t.classroom.id = :classroomId AND t.assignment.status = PUBLISHED""")
	long countOpenAssignments(@Param("classroomId") Long classroomId);

	/** Số lớp của từng giáo viên: mỗi phần tử là {@code [teacherId, count]}. */
	@Query("SELECT c.teacher.id, count(c) FROM Classroom c WHERE c.teacher.id IN :teacherIds GROUP BY c.teacher.id")
	List<Object[]> countByTeacherIds(@Param("teacherIds") Collection<Long> teacherIds);
}
