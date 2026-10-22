package com.ltmtu.ielts.user;

import java.time.Instant;

import com.ltmtu.ielts.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một phiên đăng nhập, giữ đúng một refresh token hợp lệ (lưu dạng SHA-256). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_sessions")
public class UserSession extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
	private String refreshTokenHash;

	@Column(name = "previous_token_hash", length = 64)
	private String previousTokenHash;

	@Column(name = "rotated_at")
	private Instant rotatedAt;

	@Column(length = 45)
	private String ip;

	@Column(name = "user_agent", length = 300)
	private String userAgent;

	@Column(name = "last_used_at", nullable = false)
	private Instant lastUsedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "revoke_reason", length = 30)
	private RevokeReason revokeReason;

	public boolean isActive(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void revoke(RevokeReason reason, Instant now) {
		if (revokedAt == null) {
			revokedAt = now;
			revokeReason = reason;
		}
	}
}
