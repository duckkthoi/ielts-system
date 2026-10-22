package com.ltmtu.ielts.security;

import java.security.Principal;

import com.ltmtu.ielts.user.Role;

/**
 * Người dùng của request hiện tại, lấy từ access token đã kiểm tra.
 * Controller nhận qua {@code @AuthenticationPrincipal AuthPrincipal me}; service dùng {@link #id()} để kiểm tra quyền
 * sở hữu, không tin id do client gửi.
 * <p>
 * Là {@link Principal} với tên = tên đăng nhập: phiên WebSocket dùng tên này để nhận tin riêng
 * ({@code convertAndSendToUser(username, "/queue/…")}).
 */
public record AuthPrincipal(Long id, String username, Role role, Long sessionId) implements Principal {

	@Override
	public String getName() {
		return username;
	}
}
