package com.ltmtu.ielts.grading;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ltmtu.ielts.grading.GradingEnums.GradeStatus;

public interface GradeRepository extends JpaRepository<Grade, Long> {

	Optional<Grade> findByVersionId(Long versionId);

	List<Grade> findByVersionIdIn(Collection<Long> versionIds);

	/** Kết quả đã trả (BR-13) của các bài làm, kèm version. */
	@Query("SELECT g FROM Grade g JOIN FETCH g.version v WHERE v.submission.id IN :submissionIds AND g.status = :status")
	List<Grade> findBySubmissionIds(@Param("submissionIds") Collection<Long> submissionIds,
			@Param("status") GradeStatus status);
}
