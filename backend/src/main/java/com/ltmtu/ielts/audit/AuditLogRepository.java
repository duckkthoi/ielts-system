package com.ltmtu.ielts.audit;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	/** Một dòng cho màn nhật ký, kèm tên người thực hiện (đọc bảng users — truy vấn chỉ đọc). */
	interface Row {
		Long getId();

		Long getActorId();

		String getActorUsername();

		String getActorName();

		String getAction();

		String getTargetType();

		Long getTargetId();

		String getIp();

		String getDetail();

		Instant getCreatedAt();
	}

	/** UC-07: lọc theo người, hành động, IP (tiền tố), khoảng thời gian; mới nhất trước. */
	@Query(nativeQuery = true, value = """
			SELECT l.id, l.actor_id AS actorId, u.username AS actorUsername, u.full_name AS actorName, l.action,
			       l.target_type AS targetType, l.target_id AS targetId, l.ip, l.detail::text AS detail,
			       l.created_at AS createdAt
			FROM audit_logs l LEFT JOIN users u ON u.id = l.actor_id
			WHERE (CAST(:actorId AS BIGINT) IS NULL OR l.actor_id = :actorId)
			  AND (CAST(:action AS VARCHAR) IS NULL OR l.action = :action)
			  AND (CAST(:ip AS VARCHAR) IS NULL OR l.ip LIKE :ip ESCAPE '!')
			  AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR l.created_at >= :from)
			  AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR l.created_at < :to)
			ORDER BY l.created_at DESC, l.id DESC""",
			countQuery = """
					SELECT count(*) FROM audit_logs l
					WHERE (CAST(:actorId AS BIGINT) IS NULL OR l.actor_id = :actorId)
					  AND (CAST(:action AS VARCHAR) IS NULL OR l.action = :action)
					  AND (CAST(:ip AS VARCHAR) IS NULL OR l.ip LIKE :ip ESCAPE '!')
					  AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR l.created_at >= :from)
					  AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR l.created_at < :to)""")
	Page<Row> search(@Param("actorId") Long actorId, @Param("action") String action, @Param("ip") String ipPrefix,
			@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
