package com.ltmtu.ielts.user.dto;

import java.time.Instant;

import com.ltmtu.ielts.user.RevokeReason;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.SessionRules;

/** DTO màn Phiên đăng nhập (UC-08) — docs/05-api.md mục A.3. Không có token, IP đã che một phần. */
public final class AdminSessionDtos {

	private AdminSessionDtos() {
	}

	/** Dữ liệu thô đọc từ DB (chỉ dùng trong service, không trả ra ngoài). */
	public record SessionView(Long sessionId, Long userId, String username, String fullName, Role role, String ip,
			String userAgent, Instant createdAt, Instant lastUsedAt, Instant expiresAt, Instant revokedAt,
			RevokeReason revokeReason) {
	}

	/** Một dòng trong bảng phiên. {@code current}: phiên admin đang dùng để xem màn này. */
	public record SessionRow(Long sessionId, Long userId, String username, String fullName, Role role,
			String ipMasked, String device, Instant createdAt, Instant lastUsedAt, Instant expiresAt,
			Instant revokedAt, RevokeReason revokeReason, SessionRules.Status status, boolean current) {

		public static SessionRow of(SessionView v, Long currentSessionId, Instant now) {
			return new SessionRow(v.sessionId(), v.userId(), v.username(), v.fullName(), v.role(),
					SessionRules.maskIp(v.ip()), SessionRules.shortUserAgent(v.userAgent()), v.createdAt(),
					v.lastUsedAt(), v.expiresAt(), v.revokedAt(), v.revokeReason(),
					SessionRules.status(v.revokedAt(), v.expiresAt(), now), v.sessionId().equals(currentSessionId));
		}
	}

	/** Kết quả thu hồi / dọn: số phiên bị ảnh hưởng. */
	public record AffectedCount(int count) {
	}
}
