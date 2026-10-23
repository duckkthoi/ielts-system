package com.ltmtu.ielts.user;

import java.util.List;

/** Sự kiện của module người dùng. Module realtime nghe (sau commit) để đá các kết nối WebSocket của phiên bị thu hồi. */
public final class UserEvents {

	private UserEvents() {
	}

	/** Các phiên đăng nhập vừa bị thu hồi (FR-06: đăng nhập nơi khác, đổi mật khẩu, khoá tài khoản…). */
	public record SessionsRevoked(Long userId, String username, List<Long> sessionIds, RevokeReason reason) {
	}
}
