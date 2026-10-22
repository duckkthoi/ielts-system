package com.ltmtu.ielts.user;

import java.time.Clock;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.security.JwtService;
import com.ltmtu.ielts.security.TokenHasher;
import com.ltmtu.ielts.user.dto.AuthDtos.AccountInfo;
import com.ltmtu.ielts.user.dto.AuthDtos.LoginResponse;
import com.ltmtu.ielts.user.dto.AuthDtos.TokenPair;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Đăng nhập, refresh token xoay vòng, đăng xuất, đổi mật khẩu (UC-01 → UC-03, SD-01, SD-02).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String BAD_CREDENTIALS_MSG = "Tên đăng nhập hoặc mật khẩu không đúng";
	/** Dùng khi không tìm thấy user, để thời gian phản hồi giống khi sai mật khẩu (chống dò tên đăng nhập). */
	private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BWXr7Wfqv6i/mA2Pa1rT3Z5Nf7lG";

	private final UserRepository userRepository;
	private final UserSessionRepository sessionRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AppProperties props;
	private final Clock clock;

	/** UC-01. Sai tên hoặc sai mật khẩu đều trả cùng một thông báo, không lộ trường nào sai. */
	@Transactional
	public LoginResponse login(String username, String password, ClientInfo client) {
		User user = userRepository.findByUsernameIgnoreCase(username.trim()).orElse(null);
		String hash = user != null ? user.getPasswordHash() : DUMMY_HASH;
		boolean matches = passwordEncoder.matches(password, hash);
		if (user == null || !matches) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", BAD_CREDENTIALS_MSG);
		}
		if (!user.isActive()) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED",
					"Tài khoản đã bị khoá, vui lòng liên hệ quản trị viên");
		}

		Instant now = clock.instant();
		String refreshToken = TokenHasher.newRefreshToken();
		UserSession session = new UserSession();
		session.setUser(user);
		session.setRefreshTokenHash(TokenHasher.sha256(refreshToken));
		session.setIp(client.ip());
		session.setUserAgent(client.userAgent());
		session.setLastUsedAt(now);
		session.setExpiresAt(now.plus(props.jwt().refreshTtl()));
		sessionRepository.save(session);
		user.setLastLoginAt(now);

		JwtService.IssuedToken access = jwtService.issueAccessToken(principalOf(user, session));
		log.info("Đăng nhập: user {} phiên {} từ {}", user.getId(), session.getId(), client.ip());
		return new LoginResponse(access.token(), refreshToken, access.expiresAt(), AccountInfo.from(user));
	}

	/**
	 * SD-02. Cấp cặp token mới và thu hồi refresh token cũ (xoay vòng).
	 * <ul>
	 * <li>Token cũ bị gửi lại <b>trong</b> thời gian ân hạn (vd 2 tab refresh cùng lúc) → 401
	 * {@code REFRESH_TOKEN_ROTATED}, không thu hồi gì; client lấy token mới mà tab kia đã lưu.</li>
	 * <li>Token cũ bị gửi lại <b>sau</b> thời gian ân hạn → coi là bị đánh cắp, thu hồi <b>mọi</b> phiên của user.</li>
	 * </ul>
	 * Dùng {@code noRollbackFor} để việc thu hồi vẫn được lưu dù hàm ném lỗi 401.
	 */
	@Transactional(noRollbackFor = ApiException.class)
	public TokenPair refresh(String refreshToken) {
		Instant now = clock.instant();
		String hash = TokenHasher.sha256(refreshToken);

		UserSession session = sessionRepository.findByRefreshTokenHashForUpdate(hash).orElse(null);
		if (session == null) {
			sessionRepository.findByPreviousTokenHashForUpdate(hash).ifPresent(s -> handleReuse(s, now));
			throw invalidRefresh();
		}
		if (!session.isActive(now) || !session.getUser().isActive()) {
			throw invalidRefresh();
		}

		String newToken = TokenHasher.newRefreshToken();
		session.setPreviousTokenHash(hash);
		session.setRefreshTokenHash(TokenHasher.sha256(newToken));
		session.setRotatedAt(now);
		session.setLastUsedAt(now);
		session.setExpiresAt(now.plus(props.jwt().refreshTtl()));

		JwtService.IssuedToken access = jwtService.issueAccessToken(principalOf(session.getUser(), session));
		return new TokenPair(access.token(), newToken, access.expiresAt());
	}

	private void handleReuse(UserSession session, Instant now) {
		boolean withinGrace = session.getRotatedAt() != null
				&& !now.isAfter(session.getRotatedAt().plus(props.jwt().refreshReuseGrace()));
		if (withinGrace && session.isActive(now)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_ROTATED",
					"Phiên vừa được làm mới ở nơi khác, vui lòng thử lại");
		}
		int revoked = sessionRepository.revokeAllOfUser(session.getUser().getId(), null, RevokeReason.TOKEN_REUSE,
				now);
		log.warn("Phát hiện dùng lại refresh token cũ: user {} phiên {}, thu hồi {} phiên",
				session.getUser().getId(), session.getId(), revoked);
	}

	/** UC-03. Gửi token sai hoặc đã thu hồi vẫn trả thành công (đăng xuất luôn "thành công" với client). */
	@Transactional
	public void logout(String refreshToken) {
		sessionRepository.findByRefreshTokenHashForUpdate(TokenHasher.sha256(refreshToken))
				.ifPresent(s -> s.revoke(RevokeReason.LOGOUT, clock.instant()));
	}

	@Transactional(readOnly = true)
	public AccountInfo me(AuthPrincipal principal) {
		return AccountInfo.from(loadUser(principal.id()));
	}

	/**
	 * UC-02. Đổi mật khẩu, xoá cờ bắt đổi mật khẩu (BR-02) và thu hồi các phiên <b>khác</b> phiên hiện tại.
	 */
	@Transactional
	public AccountInfo changePassword(AuthPrincipal principal, String currentPassword, String newPassword) {
		User user = loadUser(principal.id());
		if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
			throw ApiException.badRequest("WRONG_PASSWORD", "Mật khẩu hiện tại không đúng");
		}
		if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
			throw ApiException.badRequest("SAME_PASSWORD", "Mật khẩu mới phải khác mật khẩu hiện tại");
		}
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setMustChangePassword(false);
		sessionRepository.revokeAllOfUser(user.getId(), principal.sessionId(), RevokeReason.PASSWORD_CHANGED,
				clock.instant());
		log.info("Đổi mật khẩu: user {}", user.getId());
		return AccountInfo.from(user);
	}

	private User loadUser(Long id) {
		return userRepository.findById(id).orElseThrow(() -> ApiException.unauthorized("Tài khoản không tồn tại"));
	}

	private static AuthPrincipal principalOf(User user, UserSession session) {
		return new AuthPrincipal(user.getId(), user.getUsername(), user.getRole(), session.getId());
	}

	private static ApiException invalidRefresh() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
				"Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
	}
}
