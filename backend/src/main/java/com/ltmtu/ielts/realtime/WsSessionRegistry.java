package com.ltmtu.ielts.realtime;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.extern.slf4j.Slf4j;

/**
 * Các kết nối WebSocket đang mở và phiên đăng nhập của từng kết nối. Cần để đá đúng kết nối của phiên bị thu hồi
 * (FR-06). {@code simpSessionId} của STOMP chính là id của {@link WebSocketSession}.
 */
@Slf4j
@Component
public class WsSessionRegistry {

	private final Map<String, WebSocketSession> sockets = new ConcurrentHashMap<>();
	private final Map<String, AuthPrincipal> principals = new ConcurrentHashMap<>();

	/** Bọc handler WebSocket để giữ tham chiếu tới từng kết nối (đăng ký ở {@link RealtimeWebSocketConfigurer}). */
	WebSocketHandler decorate(WebSocketHandler handler) {
		return new WebSocketHandlerDecorator(handler) {
			@Override
			public void afterConnectionEstablished(WebSocketSession session) throws Exception {
				sockets.put(session.getId(), session);
				super.afterConnectionEstablished(session);
			}

			@Override
			public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
				sockets.remove(session.getId());
				super.afterConnectionClosed(session, status);
			}
		};
	}

	@EventListener
	void onConnected(SessionConnectedEvent event) {
		AuthPrincipal p = principal(event.getUser());
		String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
		if (p != null && sessionId != null) {
			principals.put(sessionId, p);
		}
	}

	@EventListener
	void onDisconnect(SessionDisconnectEvent event) {
		principals.remove(event.getSessionId());
	}

	/** Các kết nối STOMP đang mở của một phiên đăng nhập. */
	List<String> connectionsOf(Long authSessionId) {
		return principals.entrySet().stream().filter(e -> authSessionId.equals(e.getValue().sessionId()))
				.map(Map.Entry::getKey).toList();
	}

	/** Đóng một kết nối từ phía server; client sẽ nhận sự kiện đóng và không tự kết nối lại vì token đã vô hiệu. */
	void close(String connectionId) {
		WebSocketSession socket = sockets.get(connectionId);
		if (socket == null || !socket.isOpen()) {
			return;
		}
		try {
			socket.close(CloseStatus.POLICY_VIOLATION.withReason("Phiên đăng nhập đã bị thu hồi"));
		} catch (IOException e) {
			log.debug("WebSocket {}: lỗi khi đóng: {}", connectionId, e.getMessage());
		}
	}

	/** Người dùng của một khung / sự kiện STOMP (đặt ở CONNECT bởi {@code StompAuthInterceptor}). */
	static AuthPrincipal principal(Principal user) {
		if (user instanceof Authentication auth && auth.getPrincipal() instanceof AuthPrincipal p) {
			return p;
		}
		return user instanceof AuthPrincipal p ? p : null;
	}
}
