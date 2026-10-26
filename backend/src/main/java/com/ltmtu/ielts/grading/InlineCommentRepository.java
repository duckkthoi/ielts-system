package com.ltmtu.ielts.grading;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InlineCommentRepository extends JpaRepository<InlineComment, Long> {

	/** Nhận xét của một bài nộp (kèm lần nộp để kiểm tra là lần cũ); khác bài nộp → rỗng. */
	@Query("SELECT ic FROM InlineComment ic JOIN FETCH ic.grade g JOIN FETCH g.version v "
			+ "WHERE ic.id = :id AND v.submission.id = :submissionId")
	Optional<InlineComment> findInSubmission(@Param("id") Long id, @Param("submissionId") Long submissionId);
}
