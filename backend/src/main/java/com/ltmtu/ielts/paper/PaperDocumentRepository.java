package com.ltmtu.ielts.paper;

import java.time.Instant;
import java.util.List;
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

	/** Một dòng "tài liệu của nhóm" cho giáo viên (H4). */
	interface TeacherDocRow {
		Long getDocumentId();

		Long getPartId();

		String getPartTitle();

		Long getGroupId();

		String getGroupName();

		String getMembers();

		Long getLastSeq();

		Instant getUpdatedAt();

		Long getOpenComments();

		Long getTotalComments();
	}

	/** Mọi tài liệu nhóm đã mở của một bài, sửa gần nhất trước; thành viên hiện tại ghép thành chuỗi theo tên. */
	@Query(nativeQuery = true, value = """
			SELECT d.id AS documentId, d.part_id AS partId, p.title AS partTitle, g.id AS groupId, g.name AS groupName,
			       (SELECT string_agg(u.full_name, ', ' ORDER BY u.full_name)
			        FROM paper_group_members m JOIN users u ON u.id = m.student_id WHERE m.group_id = g.id) AS members,
			       d.last_seq AS lastSeq, d.updated_at AS updatedAt,
			       (SELECT count(*) FROM paper_live_comments c
			        WHERE c.document_id = d.id AND c.resolved_at IS NULL) AS openComments,
			       (SELECT count(*) FROM paper_live_comments c WHERE c.document_id = d.id) AS totalComments
			FROM paper_documents d
			JOIN paper_groups g ON g.id = d.group_id
			JOIN assignment_parts p ON p.id = d.part_id
			WHERE g.assignment_id = :assignmentId
			ORDER BY d.updated_at DESC, d.id""")
	List<TeacherDocRow> teacherDocuments(@Param("assignmentId") Long assignmentId);
}
