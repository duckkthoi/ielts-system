package com.ltmtu.ielts.security;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.ltmtu.ielts.user.SessionAuthView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Xác thực và phân quyền từng khung STOMP từ client (docs/05-api.md phần B, AI-CODING-RULES mục 11).
 * <ul>
 * <li>{@code CONNECT}: header {@code Authorization: Bearer …} phải là access token hợp lệ, phiên chưa bị thu hồi,
 * tài khoản không đang bị bắt đổi mật khẩu. Sai → khung {@code ERROR} và đóng kết nối.</li>
 * <li>{@code SUBSCRIBE}: kênh riêng của chính người dùng ({@code /user/queue/…}) luôn được; kênh {@code /topic/…}
 * (tiến độ live, online, chat — Mức 2) do các {@link StompAccessRule} quyết định (chủ bài, thành viên kênh).</li>
 * <li>{@code SEND}: chỉ khi một {@link StompAccessRule} cho phép.</li>
 * </ul>
 * Ném lỗi ở đây thì Spring trả khung {@code ERROR} cho client.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

	/** Kênh riêng mỗi người luôn được SUBSCRIBE: thông báo, FORCE_LOGOUT, lỗi của lệnh STOMP, tin mới ở kênh bài nộp. */
	static final Set<String> USER_QUEUES = Set.of("/user/queue/notifications", "/user/queue/session",
			"/user/queue/errors", "/user/queue/chat");

	private final AccessTokenVerifier verifier;
	private final List<StompAccessRule> rules;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}
		StompCommand command = accessor.getCommand();
		switch (command) {
			case CONNECT, STOMP -> accessor.setUser(authenticate(accessor));
			case SUBSCRIBE -> {
				AuthPrincipal user = requireUser(accessor);
				String destination = accessor.getDestination();
				if (!USER_QUEUES.contains(destination) && !allowed(user, destination, true)) {
					throw new MessageDeliveryException("Không có quyền nhận kênh " + destination);
				}
			}
			case SEND -> {
				AuthPrincipal user = requireUser(accessor);
				if (!allowed(user, accessor.getDestination(), false)) {
					throw new MessageDeliveryException("Không có quyền gửi tới " + accessor.getDestination());
				}
			}
			default -> {
				// UNSUBSCRIBE, DISCONNECT, ACK… không cần kiểm tra thêm.
			}
		}
		return message;
	}

	private Authentication authenticate(StompHeaderAccessor accessor) {
		SessionAuthView view = verifier.verifyHeader(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION))
				.orElseThrow(() -> new MessageDeliveryException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn"));
		if (view.mustChangePassword()) {
			throw new MessageDeliveryException("Vui lòng đổi mật khẩu trước khi tiếp tục");
		}
		log.debug("WebSocket CONNECT: user {} phiên {}", view.userId(), view.sessionId());
		return AccessTokenVerifier.authentication(view);
	}

	/** Luật đầu tiên nhận phụ trách kênh quyết định; không luật nào nhận → từ chối. */
	private boolean allowed(AuthPrincipal user, String destination, boolean subscribe) {
		if (destination == null) {
			return false;
		}
		for (StompAccessRule rule : rules) {
			Boolean decision = subscribe ? rule.canSubscribe(user, destination) : rule.canSend(user, destination);
			if (decision != null) {
				return decision;
			}
		}
		return false;
	}

	private static AuthPrincipal requireUser(StompHeaderAccessor accessor) {
		if (accessor.getUser() instanceof Authentication auth && auth.getPrincipal() instanceof AuthPrincipal p) {
			return p;
		}
		throw new MessageDeliveryException("Chưa đăng nhập");
	}
}
