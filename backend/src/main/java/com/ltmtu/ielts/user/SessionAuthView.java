package com.ltmtu.ielts.user;

import java.time.Instant;

/** Dữ liệu tối thiểu để xác thực một request: phiên + trạng thái tài khoản. */
public record SessionAuthView(
		Long sessionId,
		Instant revokedAt,
		Long userId,
		String username,
		Role role,
		boolean active,
		boolean mustChangePassword) {

	public boolean usable() {
		return revokedAt == null && active;
	}
}
