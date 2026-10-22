package com.ltmtu.ielts.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ltmtu.ielts.user.SessionAuthView;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Xác thực request bằng access token trong header {@code Authorization: Bearer …}.
 * <p>
 * Ngoài chữ ký và hạn của JWT, filter kiểm tra <b>phiên còn hiệu lực và tài khoản còn hoạt động</b> trong DB, để
 * đăng xuất / đổi mật khẩu / khoá tài khoản có tác dụng ngay, không phải chờ access token hết hạn.
 * Tài khoản đang bị bắt đổi mật khẩu chỉ được gọi {@code /api/auth/**}.
 * <p>
 * Không đăng ký làm {@code @Component} để Spring Boot không tự thêm vào servlet filter chain lần thứ hai; được tạo
 * trong {@code SecurityConfig}.
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final String AUTH_PATH_PREFIX = "/api/auth/";

	private final AccessTokenVerifier verifier;
	private final SecurityErrorWriter errorWriter;

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {
		SessionAuthView view = verifier.verifyHeader(req.getHeader(HttpHeaders.AUTHORIZATION)).orElse(null);
		if (view == null) {
			// Thiếu token / token sai / hết hạn / phiên bị thu hồi: đi tiếp như chưa đăng nhập → entry point trả 401.
			chain.doFilter(req, res);
			return;
		}

		if (view.mustChangePassword() && !req.getRequestURI().startsWith(AUTH_PATH_PREFIX)) {
			errorWriter.write(req, res, HttpStatus.FORBIDDEN, "PASSWORD_CHANGE_REQUIRED",
					"Vui lòng đổi mật khẩu trước khi tiếp tục");
			return;
		}

		SecurityContextHolder.getContext().setAuthentication(AccessTokenVerifier.authentication(view));
		chain.doFilter(req, res);
	}
}
