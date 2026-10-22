package com.ltmtu.ielts.grading;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ltmtu.ielts.grading.GradingEnums.CriterionGroup;

public interface CommentBankRepository extends JpaRepository<CommentBankItem, Long> {

	/**
	 * Nhận xét của giáo viên (hay dùng nhất lên đầu) rồi tới nhận xét mẫu của hệ thống. Lọc theo nhóm tiêu chí thì
	 * vẫn kèm các nhận xét dùng chung ({@code criterion IS NULL}).
	 */
	@Query("SELECT c FROM CommentBankItem c WHERE (c.owner IS NULL OR c.owner.id = :ownerId) "
			+ "AND (:criterion IS NULL OR c.criterion = :criterion OR c.criterion IS NULL) "
			+ "AND (:keyword IS NULL OR lower(c.content) LIKE :keyword ESCAPE '!') "
			+ "ORDER BY CASE WHEN c.owner IS NULL THEN 1 ELSE 0 END, c.useCount DESC, c.lastUsedAt DESC NULLS LAST, c.id")
	List<CommentBankItem> findUsableBy(@Param("ownerId") Long ownerId, @Param("criterion") CriterionGroup criterion,
			@Param("keyword") String keyword);

	@Query("SELECT c FROM CommentBankItem c WHERE c.id = :id AND (c.owner IS NULL OR c.owner.id = :ownerId)")
	Optional<CommentBankItem> findUsable(@Param("id") Long id, @Param("ownerId") Long ownerId);

	long countByOwnerId(Long ownerId);
}
