package com.ltmtu.ielts.audit;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Giữ IP client của request HTTP hiện tại trong luồng, để {@link AuditService} ghi IP mà service nghiệp vụ không phải
 * đọc {@code HttpServletRequest}. Sau nginx, IP thật lấy từ {@code X-Forwarded-For} nhờ
 * {@code server.forward-headers-strategy=framework} (chỉ bật ở profile docker, nơi backend không lộ ra ngoài).
 */
@Component
public class RequestIp extends OncePerRequestFilter {

	private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

	/** IP của request đang xử lý trên luồng này; {@code null} ngoài request HTTP (job, TCP). */
	public static String current() {
		return CURRENT.get();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		CURRENT.set(request.getRemoteAddr());
		try {
			chain.doFilter(request, response);
		} finally {
			CURRENT.remove();
		}
	}
}
