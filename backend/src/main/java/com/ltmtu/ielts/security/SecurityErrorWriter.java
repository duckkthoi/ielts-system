package com.ltmtu.ielts.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.ltmtu.ielts.common.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * Lỗi phát sinh trong filter chain của Spring Security (chưa tới controller) cũng trả đúng khung
 * {@link ErrorResponse} như mọi API khác: HTTP 200, mã lỗi thật (401, 403…) trong trường {@code status}.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	/** Chưa đăng nhập, token sai / hết hạn, hoặc phiên đã bị thu hồi. */
	@Override
	public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
			throws IOException {
		write(req, res, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Vui lòng đăng nhập để tiếp tục");
	}

	@Override
	public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
		write(req, res, HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này");
	}

	public void write(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String code, String message)
			throws IOException {
		res.setStatus(HttpServletResponse.SC_OK);
		res.setCharacterEncoding("UTF-8");
		res.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(res.getOutputStream(),
				ErrorResponse.of(status.value(), code, message, req.getRequestURI()));
	}
}
