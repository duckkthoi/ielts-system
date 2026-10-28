package com.ltmtu.ielts.paper;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaperDocUpdateRepository extends JpaRepository<PaperDocUpdate, PaperDocUpdate.Id> {

	/** Các update còn nội dung sau một số thứ tự, theo thứ tự cấp. */
	@Query("""
			SELECT u FROM PaperDocUpdate u
			WHERE u.id.documentId = :documentId AND u.id.seq > :afterSeq AND u.payload IS NOT NULL
			ORDER BY u.id.seq""")
	List<PaperDocUpdate> findAfter(@Param("documentId") Long documentId, @Param("afterSeq") long afterSeq);

	long countByIdDocumentId(Long documentId);

	/** Đã gộp vào snapshot: bỏ nội dung, giữ dòng cho thống kê đóng góp. */
	@Modifying(flushAutomatically = true)
	@Query("""
			UPDATE PaperDocUpdate u SET u.payload = NULL
			WHERE u.id.documentId = :documentId AND u.id.seq <= :upToSeq AND u.payload IS NOT NULL""")
	int compact(@Param("documentId") Long documentId, @Param("upToSeq") long upToSeq);
}
