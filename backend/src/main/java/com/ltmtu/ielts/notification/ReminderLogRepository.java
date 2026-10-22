package com.ltmtu.ielts.notification;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, ReminderLog.Id> {

	/** Một (phần, sinh viên) cần nhắc ở một mốc. */
	interface Candidate {
		Long getPartId();

		Long getStudentId();

		Long getAssignmentId();

		String getAssignmentTitle();

		String getPartTitle();

		Instant getDueAt();

		String getKind();
	}

	/**
	 * Ai cần nhắc lúc này (SD-10, BR-14). Hai nhánh:
	 * <ol>
	 * <li><b>hạn nộp</b>: phần của bài đang mở, hạn áp dụng (hạn phần → hạn bài) trong {@code (now, soonEnd]};
	 * người nhận (giao trực tiếp ∪ thành viên lớp) <b>chưa nộp</b> (chưa có bài làm, hoặc mới nháp);</li>
	 * <li><b>hạn sửa</b>: bài làm đang "Cần sửa" có hạn sửa trong {@code (now, soonEnd]}.</li>
	 * </ol>
	 * Mốc: còn ≤ {@code urgentEnd} → H2 (REVISION_H2), ngược lại H24 (REVISION_H24). Bỏ những ai đã có
	 * {@code reminder_log} ở đúng mốc đó. Native SQL vì cần UNION / CTE.
	 */
	@Query(nativeQuery = true, value = """
			WITH due_parts AS (
			    SELECT p.id AS part_id, a.id AS assignment_id, a.title AS assignment_title, p.title AS part_title,
			           COALESCE(p.part_due_at, a.due_at) AS due_at
			    FROM assignment_parts p JOIN assignments a ON a.id = p.assignment_id
			    WHERE a.status = 'PUBLISHED' AND (a.open_at IS NULL OR a.open_at <= :now)
			      AND COALESCE(p.part_due_at, a.due_at) > :now AND COALESCE(p.part_due_at, a.due_at) <= :soonEnd
			), recipients AS (
			    SELECT t.assignment_id, t.student_id FROM assignment_targets t
			    WHERE t.student_id IS NOT NULL AND t.assignment_id IN (SELECT assignment_id FROM due_parts)
			    UNION
			    SELECT t.assignment_id, m.student_id FROM assignment_targets t
			    JOIN class_members m ON m.classroom_id = t.classroom_id
			    WHERE t.assignment_id IN (SELECT assignment_id FROM due_parts)
			), candidates AS (
			    SELECT d.part_id, r.student_id, d.assignment_id, d.assignment_title, d.part_title, d.due_at,
			           CASE WHEN d.due_at <= :urgentEnd THEN 'H2' ELSE 'H24' END AS kind
			    FROM due_parts d JOIN recipients r ON r.assignment_id = d.assignment_id
			    LEFT JOIN submissions s ON s.part_id = d.part_id AND s.student_id = r.student_id
			    WHERE s.id IS NULL OR s.status IN ('NOT_STARTED', 'DRAFT')
			    UNION ALL
			    SELECT s.part_id, s.student_id, a.id, a.title, p.title, s.revision_due_at,
			           CASE WHEN s.revision_due_at <= :urgentEnd THEN 'REVISION_H2' ELSE 'REVISION_H24' END
			    FROM submissions s JOIN assignment_parts p ON p.id = s.part_id JOIN assignments a ON a.id = p.assignment_id
			    WHERE s.status = 'REVISION_REQUESTED' AND a.status = 'PUBLISHED'
			      AND s.revision_due_at > :now AND s.revision_due_at <= :soonEnd
			)
			SELECT c.part_id AS partId, c.student_id AS studentId, c.assignment_id AS assignmentId,
			       c.assignment_title AS assignmentTitle, c.part_title AS partTitle, c.due_at AS dueAt, c.kind AS kind
			FROM candidates c
			WHERE NOT EXISTS (SELECT 1 FROM reminder_log l
			                  WHERE l.part_id = c.part_id AND l.student_id = c.student_id AND l.kind = c.kind)
			ORDER BY c.part_id, c.kind, c.student_id""")
	List<Candidate> findCandidates(@Param("now") Instant now, @Param("urgentEnd") Instant urgentEnd,
			@Param("soonEnd") Instant soonEnd);

	/** Ghi "đã nhắc"; trả 1 nếu chèn được, 0 nếu đã có (lần chạy khác vừa nhắc) — AI-CODING-RULES mục 10. */
	@Modifying
	@Query(nativeQuery = true, value = """
			INSERT INTO reminder_log (part_id, student_id, kind, sent_at) VALUES (:partId, :studentId, :kind, :now)
			ON CONFLICT DO NOTHING""")
	int insertIfAbsent(@Param("partId") Long partId, @Param("studentId") Long studentId, @Param("kind") String kind,
			@Param("now") Instant now);

	/** Bị yêu cầu sửa lần mới → xoá mốc nhắc hạn sửa cũ, để vòng sửa này vẫn được nhắc. */
	@Modifying
	@Query(nativeQuery = true, value = """
			DELETE FROM reminder_log WHERE part_id = :partId AND student_id = :studentId
			  AND kind IN ('REVISION_H24', 'REVISION_H2')""")
	int resetRevision(@Param("partId") Long partId, @Param("studentId") Long studentId);
}
