package com.ltmtu.ielts.user;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.audit.AuditAction;
import com.ltmtu.ielts.audit.AuditService;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.Pageables;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.dto.AdminSessionDtos.AffectedCount;
import com.ltmtu.ielts.user.dto.AdminSessionDtos.SessionRow;
import com.ltmtu.ielts.user.dto.AdminSessionDtos.SessionView;

import lombok.RequiredArgsConstructor;

/**
 * Quản lý phiên đăng nhập của admin (UC-08). Thu hồi đi qua {@link SessionRevoker} nên thiết bị đang mở WebSocket nhận
 * {@code FORCE_LOGOUT ADMIN_FORCED} ngay; REST / refresh bằng phiên đó bị từ chối ở request sau. Nhật ký chỉ ghi id
 * phiên / người dùng và số lượng — không ghi token, IP hay user-agent.
 */
@Service
@RequiredArgsConstructor
public class AdminSessionService {

	private static final Map<String, String> SORTABLE = Map.of(
			"createdAt", "createdAt",
			"lastUsedAt", "lastUsedAt",
			"expiresAt", "expiresAt",
			"username", "user.username");
	private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "lastUsedAt");
	/** Mốc thay cho "không lọc thời gian". */
	private static final Instant MIN_TIME = Instant.EPOCH;
	private static final Instant MAX_TIME = Instant.parse("3000-01-01T00:00:00Z");

	private final UserSessionRepository sessionRepository;
	private final UserRepository userRepository;
	private final SessionRevoker sessionRevoker;
	private final AuditService audit;
	private final Clock clock;

	@Transactional(readOnly = true)
	public Page<SessionRow> search(AuthPrincipal admin, String keyword, Role role, SessionRules.Status status,
			Instant from, Instant to, Pageable pageable) {
		if (from != null && to != null && !from.isBefore(to)) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Thời điểm bắt đầu phải trước thời điểm kết thúc");
		}
		Instant now = clock.instant();
		return sessionRepository.search(Pageables.likePattern(keyword), role, status == null ? null : status.name(),
				from == null ? MIN_TIME : from, to == null ? MAX_TIME : to, now, Pageables.sanitize(pageable, SORTABLE, DEFAULT_SORT))
				.map(v -> SessionRow.of(v, admin.sessionId(), now));
	}

	/** Thu hồi một phiên. Không thu hồi phiên đang dùng (dùng Đăng xuất); phiên đã thu hồi → trả lại như cũ. */
	@Transactional
	public SessionRow revoke(AuthPrincipal admin, Long sessionId) {
		if (sessionId.equals(admin.sessionId())) {
			throw ApiException.badRequest("CURRENT_SESSION",
					"Không thu hồi được phiên bạn đang dùng — hãy dùng Đăng xuất");
		}
		UserSession session = sessionRepository.findByIdForUpdate(sessionId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy phiên đăng nhập"));
		Instant now = clock.instant();
		if (session.getRevokedAt() == null && !session.isActive(now)) {
			throw ApiException.conflict("INVALID_STATE", "Phiên đã hết hạn, không cần thu hồi");
		}
		if (sessionRevoker.revokeOne(session, RevokeReason.ADMIN_FORCED, now)) {
			audit.record(admin.id(), AuditAction.REVOKE_SESSION, "USER_SESSION", sessionId,
					Map.of("userId", session.getUser().getId()));
		}
		User user = session.getUser();
		SessionView view = new SessionView(session.getId(), user.getId(), user.getUsername(), user.getFullName(),
				user.getRole(), session.getIp(), session.getUserAgent(), session.getCreatedAt(), session.getLastUsedAt(),
				session.getExpiresAt(), session.getRevokedAt(), session.getRevokeReason());
		return SessionRow.of(view, admin.sessionId(), now);
	}

	/** Thu hồi mọi phiên còn hiệu lực của một tài khoản (trừ phiên admin đang dùng nếu là chính mình). */
	@Transactional
	public AffectedCount revokeAllOfUser(AuthPrincipal admin, Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
		Long keep = userId.equals(admin.id()) ? admin.sessionId() : null;
		int count = sessionRevoker.revokeAll(user, keep, RevokeReason.ADMIN_FORCED, clock.instant());
		if (count > 0) {
			audit.record(admin.id(), AuditAction.REVOKE_USER_SESSIONS, "USER", userId, Map.of("revokedSessions", count));
		}
		return new AffectedCount(count);
	}

	/** Xoá các phiên đã hết hạn khỏi bảng (phiên đã thu hồi nhưng chưa hết hạn được giữ để phát hiện dùng lại token). */
	@Transactional
	public AffectedCount cleanupExpired(AuthPrincipal admin) {
		int count = sessionRepository.deleteExpired(clock.instant());
		audit.record(admin.id(), AuditAction.CLEANUP_SESSIONS, "USER_SESSION", null, Map.of("deleted", count));
		return new AffectedCount(count);
	}
}
