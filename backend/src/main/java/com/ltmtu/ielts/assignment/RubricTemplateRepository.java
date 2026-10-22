package com.ltmtu.ielts.assignment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;

public interface RubricTemplateRepository extends JpaRepository<RubricTemplate, Long> {

	/** Mẫu của hệ thống + mẫu riêng của giáo viên; {@code taskType} NULL = mọi loại. */
	@Query("""
			SELECT DISTINCT t FROM RubricTemplate t LEFT JOIN FETCH t.criteria
			WHERE (t.owner IS NULL OR t.owner.id = :teacherId)
			  AND (:taskType IS NULL OR t.taskType = :taskType)
			ORDER BY t.id""")
	List<RubricTemplate> findAvailable(@Param("teacherId") Long teacherId, @Param("taskType") TaskType taskType);
}
