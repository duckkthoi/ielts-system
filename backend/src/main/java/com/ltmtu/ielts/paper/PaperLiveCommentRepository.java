package com.ltmtu.ielts.paper;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaperLiveCommentRepository extends JpaRepository<PaperLiveComment, Long> {

	@Query("SELECT c FROM PaperLiveComment c JOIN FETCH c.author LEFT JOIN FETCH c.resolvedBy "
			+ "WHERE c.document.id = :documentId ORDER BY c.startOffset, c.id")
	List<PaperLiveComment> findByDocument(@Param("documentId") Long documentId);

	@Query("SELECT c FROM PaperLiveComment c JOIN FETCH c.author LEFT JOIN FETCH c.resolvedBy JOIN FETCH c.document d "
			+ "WHERE c.id = :id AND d.id = :documentId")
	Optional<PaperLiveComment> findInDocument(@Param("id") Long id, @Param("documentId") Long documentId);
}
