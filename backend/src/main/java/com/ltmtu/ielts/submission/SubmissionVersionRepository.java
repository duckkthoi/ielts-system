package com.ltmtu.ielts.submission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionVersionRepository extends JpaRepository<SubmissionVersion, Long> {

	List<SubmissionVersion> findBySubmissionIdOrderByVersionNoDesc(Long submissionId);

	Optional<SubmissionVersion> findBySubmissionIdAndVersionNo(Long submissionId, int versionNo);

	/** Version mới nhất của mỗi bài làm (để biết lần nộp gần nhất có muộn không). */
	@Query("SELECT v FROM SubmissionVersion v WHERE v.submission.id IN :submissionIds "
			+ "AND v.versionNo = v.submission.currentVersion")
	List<SubmissionVersion> findLatest(@Param("submissionIds") Collection<Long> submissionIds);
}
