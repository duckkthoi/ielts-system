package com.ltmtu.ielts.audit;

import java.time.Instant;

import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Một dòng nhật ký. Chỉ thêm, không sửa. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "actor_id")
	private Long actorId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private AuditAction action;

	@Column(name = "target_type", length = 30)
	private String targetType;

	@Column(name = "target_id")
	private Long targetId;

	@Column(length = 45)
	private String ip;

	/** JSON dạng chuỗi; ép kiểu sang {@code jsonb} khi ghi (không cần thư viện JSON của Hibernate). */
	@Column(columnDefinition = "jsonb")
	@ColumnTransformer(write = "?::jsonb")
	private String detail;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	static AuditLog of(Long actorId, AuditAction action, String targetType, Long targetId, String ip, String detail,
			Instant createdAt) {
		AuditLog log = new AuditLog();
		log.actorId = actorId;
		log.action = action;
		log.targetType = targetType;
		log.targetId = targetId;
		log.ip = ip;
		log.detail = detail;
		log.createdAt = createdAt;
		return log;
	}
}
