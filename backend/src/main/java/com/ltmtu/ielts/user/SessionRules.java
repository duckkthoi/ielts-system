package com.ltmtu.ielts.user;

import java.time.Instant;

/**
 * Quy tắc hiển thị phiên đăng nhập ở màn quản trị (UC-08) — class thuần, có test. Chỉ trả thông tin đã làm gọn: IP che
 * một phần, trình duyệt / hệ điều hành rút gọn; không bao giờ có token.
 */
public final class SessionRules {

	/** Trạng thái hiển thị của một phiên. */
	public enum Status {
		ACTIVE, EXPIRED, REVOKED
	}

	private SessionRules() {
	}

	public static Status status(Instant revokedAt, Instant expiresAt, Instant now) {
		if (revokedAt != null) {
			return Status.REVOKED;
		}
		return expiresAt.isAfter(now) ? Status.ACTIVE : Status.EXPIRED;
	}

	/** IPv4: che octet cuối ({@code 192.168.1.*}); IPv6: giữ 3 nhóm đầu ({@code 2001:db8:85a3:*}). */
	public static String maskIp(String ip) {
		if (ip == null || ip.isBlank()) {
			return null;
		}
		String value = ip.trim();
		if (value.contains(":")) {
			String[] groups = value.split(":", -1);
			StringBuilder sb = new StringBuilder();
			for (int i = 0, kept = 0; i < groups.length && kept < 3; i++) {
				if (groups[i].isEmpty()) {
					break;
				}
				sb.append(groups[i]).append(':');
				kept++;
			}
			return sb.length() == 0 ? "*" : sb + "*";
		}
		int dot = value.lastIndexOf('.');
		return dot < 0 ? "*" : value.substring(0, dot) + ".*";
	}

	/** Rút gọn User-Agent thành "Trình duyệt · Hệ điều hành" (vd "Chrome · Windows"); không nhận ra → "Khác". */
	public static String shortUserAgent(String ua) {
		if (ua == null || ua.isBlank()) {
			return "Không rõ";
		}
		String browser = browser(ua);
		String os = os(ua);
		if (browser == null && os == null) {
			return "Khác";
		}
		if (browser == null) {
			return os;
		}
		return os == null ? browser : browser + " · " + os;
	}

	private static String browser(String ua) {
		// Thứ tự quan trọng: Edge / Opera / Chrome đều có chữ "Chrome", Chrome có chữ "Safari".
		if (ua.contains("Edg/")) {
			return "Edge";
		}
		if (ua.contains("OPR/")) {
			return "Opera";
		}
		if (ua.contains("CocCoc")) {
			return "Cốc Cốc";
		}
		if (ua.contains("Firefox/")) {
			return "Firefox";
		}
		if (ua.contains("Chrome/") || ua.contains("CriOS/")) {
			return "Chrome";
		}
		if (ua.contains("Safari/")) {
			return "Safari";
		}
		if (ua.contains("k6/")) {
			return "k6";
		}
		if (ua.startsWith("Java") || ua.contains("Apache-HttpClient")) {
			return "Ứng dụng Java";
		}
		if (ua.startsWith("curl/")) {
			return "curl";
		}
		return null;
	}

	private static String os(String ua) {
		if (ua.contains("Android")) {
			return "Android";
		}
		if (ua.contains("iPhone") || ua.contains("iPad")) {
			return "iOS";
		}
		if (ua.contains("Windows")) {
			return "Windows";
		}
		if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
			return "macOS";
		}
		if (ua.contains("Linux")) {
			return "Linux";
		}
		return null;
	}
}
