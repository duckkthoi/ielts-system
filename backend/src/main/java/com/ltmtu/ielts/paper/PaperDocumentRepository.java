package com.ltmtu.ielts.paper;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PaperDocumentRepository extends JpaRepository<PaperDocument, Long> {

	Optional<PaperDocument> findByPartIdAndGroupId(Long partId, Long groupId);

	/** Tạo tài liệu nếu chưa có — hai thành viên mở cùng lúc chỉ tạo một (UNIQUE part + group). */
	@Modifying
	@Query(nativeQuery = true, value = """
			INSERT INTO paper_documents (part_id, group_id, created_at, updated_at)
			VALUES (:partId, :groupId, :now, :now)
			ON CONFLICT (part_id, group_id) DO NOTHING""")
	int insertIfAbsent(@Param("partId") Long partId, @Param("groupId") Long groupId, @Param("now") Instant now);

	/** Tài liệu kèm phần, bài tập (giáo viên) và nhóm — đủ để kiểm quyền. */
	@Query("""
			SELECT d FROM PaperDocument d JOIN FETCH d.part p JOIN FETCH p.assignment a JOIN FETCH a.teacher
			JOIN FETCH d.group WHERE d.id = :id""")
	Optional<PaperDocument> findFull(@Param("id") Long id);

	/** Khoá dòng khi cấp số thứ tự update / lưu snapshot: các update đồng thời nhận seq liên tiếp, không trùng. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT d FROM PaperDocument d WHERE d.id = :id")
	Optional<PaperDocument> findForUpdate(@Param("id") Long id);

	/** Trạng thái bài làm của nhóm cho giai đoạn này (chỉ đọc bảng của module bài nộp); chưa có → rỗng. */
	@Query(nativeQuery = true, value = "SELECT status FROM submissions WHERE part_id = :partId AND group_id = :groupId")
	Optional<String> submissionStatus(@Param("partId") Long partId, @Param("groupId") Long groupId);
}
