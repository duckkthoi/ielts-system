package com.ltmtu.ielts.user;

import jakarta.servlet.http.HttpServletRequest;

/** IP và trình duyệt của client, lưu vào phiên đăng nhập (UC-08) và audit log. */
public record ClientInfo(String ip, String userAgent) {

	private static final int MAX_UA = 300;

	public static ClientInfo from(HttpServletRequest req) {
		String ua = req.getHeader("User-Agent");
		if (ua != null && ua.length() > MAX_UA) {
			ua = ua.substring(0, MAX_UA);
		}
		return new ClientInfo(req.getRemoteAddr(), ua);
	}
}
