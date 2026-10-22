package com.ltmtu.ielts.security;

import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.ltmtu.ielts.user.SessionAuthView;
import com.ltmtu.ielts.user.UserSessionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Kiểm tra access token — dùng chung cho REST ({@link JwtAuthFilter}) và WebSocket ({@link StompAuthInterceptor}).
 * Ngoài chữ ký và hạn của JWT, còn kiểm tra phiên ({@code sid}) chưa bị thu hồi và tài khoản còn hoạt động, để đăng
 * xuất / đổi mật khẩu / khoá tài khoản có tác dụng ngay (docs/05-api.md A.2).
 */
@Component
@RequiredArgsConstructor
public class AccessTokenVerifier {

	private static final String BEARER = "Bearer ";

	private final JwtService jwtService;
	private final UserSessionRepository sessionRepository;

	/** Giá trị header {@code Authorization} → phiên hợp lệ; rỗng nếu thiếu, sai, hết hạn hoặc đã bị thu hồi. */
	public Optional<SessionAuthView> verifyHeader(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
			return Optional.empty();
		}
		return jwtService.parse(authorizationHeader.substring(BEARER.length()).trim())
				.flatMap(p -> sessionRepository.findAuthView(p.sessionId()).filter(v -> v.userId().equals(p.id())))
				.filter(SessionAuthView::usable);
	}

	/** Đối tượng xác thực của Spring Security cho phiên đã kiểm tra. */
	public static UsernamePasswordAuthenticationToken authentication(SessionAuthView view) {
		AuthPrincipal principal = new AuthPrincipal(view.userId(), view.username(), view.role(), view.sessionId());
		return new UsernamePasswordAuthenticationToken(principal, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + view.role().name())));
	}
}
