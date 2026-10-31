package com.ltmtu.ielts.user;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Chỗ duy nhất thu hồi hàng loạt phiên của một tài khoản. Ngoài việc đánh dấu thu hồi (REST bị chặn ngay ở request
 * sau), còn phát {@link UserEvents.SessionsRevoked} để thiết bị đang mở WebSocket nhận {@code FORCE_LOGOUT} và bị
 * ngắt kết nối (FR-06, docs/05 B.2).
 */
@Component
@RequiredArgsConstructor
public class SessionRevoker {

	private final UserSessionRepository sessionRepository;
	private final ApplicationEventPublisher events;

	/** Thu hồi mọi phiên còn hiệu lực của {@code user}, trừ {@code exceptSessionId}. Trả về số phiên bị thu hồi. */
	@Transactional(propagation = Propagation.MANDATORY)
	public int revokeAll(User user, Long exceptSessionId, RevokeReason reason, Instant now) {
		List<Long> ids = sessionRepository.findActiveIds(user.getId(), exceptSessionId);
		if (ids.isEmpty()) {
			return 0;
		}
		sessionRepository.revokeAllOfUser(user.getId(), exceptSessionId, reason, now);
		events.publishEvent(new UserEvents.SessionsRevoked(user.getId(), user.getUsername(), ids, reason));
		return ids.size();
	}

	/** Thu hồi một phiên (admin buộc đăng xuất). Phiên đã thu hồi thì bỏ qua và trả {@code false}. */
	@Transactional(propagation = Propagation.MANDATORY)
	public boolean revokeOne(UserSession session, RevokeReason reason, Instant now) {
		if (session.getRevokedAt() != null) {
			return false;
		}
		session.revoke(reason, now);
		User user = session.getUser();
		events.publishEvent(
				new UserEvents.SessionsRevoked(user.getId(), user.getUsername(), List.of(session.getId()), reason));
		return true;
	}
}
