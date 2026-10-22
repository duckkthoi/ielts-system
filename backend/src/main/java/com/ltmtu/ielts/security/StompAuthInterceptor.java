package com.ltmtu.ielts.security;

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
 * <li>{@code SUBSCRIBE}: chỉ các kênh riêng của chính người dùng ở bảng B.2. Kênh {@code /topic/…} (tiến độ live,
 * online, chat) mở ở Mức 2 cùng quy tắc chủ bài / thành viên kênh.</li>
 * <li>{@code SEND}: Mức 1 chưa có lệnh nào client được gửi lên.</li>
 * </ul>
 * Ném lỗi ở đây thì Spring trả khung {@code ERROR} cho client.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

	/** Kênh riêng mỗi người được SUBSCRIBE (Mức 1 + {@code /user/queue/session} cho FORCE_LOGOUT ở Mức 2). */
	static final Set<String> USER_QUEUES = Set.of("/user/queue/notifications", "/user/queue/session");

	private final AccessTokenVerifier verifier;

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
				requireUser(accessor);
				if (!USER_QUEUES.contains(accessor.getDestination())) {
					throw new MessageDeliveryException("Không có quyền nhận kênh " + accessor.getDestination());
				}
			}
			case SEND -> {
				requireUser(accessor);
				throw new MessageDeliveryException("Không có quyền gửi tới " + accessor.getDestination());
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

	private static void requireUser(StompHeaderAccessor accessor) {
		if (accessor.getUser() == null) {
			throw new MessageDeliveryException("Chưa đăng nhập");
		}
	}
}
