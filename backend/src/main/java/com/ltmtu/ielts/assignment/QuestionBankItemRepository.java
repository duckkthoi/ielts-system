package com.ltmtu.ielts.assignment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;

public interface QuestionBankItemRepository extends JpaRepository<QuestionBankItem, Long> {

	@Query(value = """
			SELECT q.id FROM QuestionBankItem q
			WHERE q.teacher.id = :teacherId
			  AND (:taskType IS NULL OR q.taskType = :taskType)
			  AND (:keyword IS NULL OR lower(q.title) LIKE :keyword ESCAPE '!'
			       OR lower(q.prompt) LIKE :keyword ESCAPE '!')
			  AND (:tag IS NULL OR :tag MEMBER OF q.tags)""",
			countQuery = """
					SELECT count(q) FROM QuestionBankItem q
					WHERE q.teacher.id = :teacherId
					  AND (:taskType IS NULL OR q.taskType = :taskType)
					  AND (:keyword IS NULL OR lower(q.title) LIKE :keyword ESCAPE '!'
					       OR lower(q.prompt) LIKE :keyword ESCAPE '!')
					  AND (:tag IS NULL OR :tag MEMBER OF q.tags)""")
	Page<Long> searchIds(@Param("teacherId") Long teacherId, @Param("taskType") TaskType taskType,
			@Param("keyword") String keyword, @Param("tag") String tag, Pageable pageable);

	@Query("SELECT DISTINCT q FROM QuestionBankItem q LEFT JOIN FETCH q.tags WHERE q.id IN :ids")
	List<QuestionBankItem> findSummaries(@Param("ids") Collection<Long> ids);

	Optional<QuestionBankItem> findByIdAndTeacherId(Long id, Long teacherId);
}
