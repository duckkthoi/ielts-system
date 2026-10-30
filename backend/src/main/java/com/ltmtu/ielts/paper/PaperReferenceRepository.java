package com.ltmtu.ielts.paper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaperReferenceRepository extends JpaRepository<PaperReference, Long> {

	/** Thư viện của nhóm theo thứ tự thêm, kèm người thêm (một câu, tránh N+1). */
	@Query("SELECT r FROM PaperReference r LEFT JOIN FETCH r.createdBy WHERE r.group.id = :groupId ORDER BY r.id")
	List<PaperReference> findByGroup(@Param("groupId") Long groupId);

	@Query("SELECT r FROM PaperReference r LEFT JOIN FETCH r.createdBy WHERE r.id = :id AND r.group.id = :groupId")
	Optional<PaperReference> findInGroup(@Param("id") Long id, @Param("groupId") Long groupId);

	long countByGroupId(Long groupId);

	/** DOI trong danh sách đã có trong thư viện nhóm (để bỏ qua khi nhập nhiều mục). */
	@Query("SELECT r.doi FROM PaperReference r WHERE r.group.id = :groupId AND r.doi IN :dois")
	Set<String> existingDois(@Param("groupId") Long groupId, @Param("dois") Collection<String> dois);

	boolean existsByGroupIdAndDoiAndIdNot(Long groupId, String doi, Long id);

	boolean existsByGroupIdAndDoi(Long groupId, String doi);
}
