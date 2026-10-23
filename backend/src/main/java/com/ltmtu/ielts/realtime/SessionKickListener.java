package com.ltmtu.ielts.realtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ltmtu.ielts.user.UserEvents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Một tài khoản một phiên (FR-06, SD-01): phiên bị thu hồi mà đang mở WebSocket thì nhận {@code FORCE_LOGOUT} trên
 * {@code /user/queue/session} rồi bị server đóng kết nối. Chạy <b>sau commit</b> (chỉ đẩy tin, không ghi DB), và gửi
 * riêng tới từng kết nối của phiên cũ — thiết bị vừa đăng nhập không nhận nhầm.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionKickListener {

	/** Chờ một chút trước khi đóng, để khung FORCE_LOGOUT kịp tới client. */
	private static final long CLOSE_DELAY_MS = 500;

	private final WsSessionRegistry registry;
	private final SimpMessagingTemplate messaging;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSessionsRevoked(UserEvents.SessionsRevoked event) {
		for (Long authSessionId : event.sessionIds()) {
			for (String connectionId : registry.connectionsOf(authSessionId)) {
				SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
				headers.setSessionId(connectionId);
				headers.setLeaveMutable(true);
				messaging.convertAndSendToUser(event.username(), "/queue/session",
						RealtimeDtos.ForceLogout.of(event.reason().name()), headers.getMessageHeaders());
				CompletableFuture.delayedExecutor(CLOSE_DELAY_MS, TimeUnit.MILLISECONDS)
						.execute(() -> registry.close(connectionId));
				log.info("FORCE_LOGOUT: user {} phiên {} ({})", event.userId(), authSessionId, event.reason());
			}
		}
	}
}
