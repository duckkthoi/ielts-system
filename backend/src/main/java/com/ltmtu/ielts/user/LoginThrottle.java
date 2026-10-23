package com.ltmtu.ielts.user;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.ltmtu.ielts.common.SlidingWindowLimiter;
import com.ltmtu.ielts.config.AppProperties;

/**
 * Chặn dò mật khẩu (NFR-09, UC-01 E3): tối đa N lần đăng nhập sai trong cửa sổ thời gian, tính <b>riêng cho mỗi IP</b>
 * (chống một máy dò nhiều tài khoản) và <b>mỗi tài khoản</b> (chống nhiều máy dò một tài khoản). Đăng nhập đúng chỉ
 * xoá bộ đếm của tài khoản, không xoá của IP — để kẻ dò không "rửa" bộ đếm IP bằng một tài khoản thật của mình.
 */
@Component
public class LoginThrottle {

	private final SlidingWindowLimiter limiter;

	public LoginThrottle(AppProperties props, Clock clock) {
		this.limiter = new SlidingWindowLimiter(props.rateLimit().loginMaxFailures(), props.rateLimit().loginWindow(),
				clock);
	}

	/** {@code null} nếu được thử; nếu bị chặn thì là thời gian phải chờ. */
	public Duration blockedFor(String username, String ip) {
		Duration wait = Duration.ZERO;
		for (String key : new String[] { userKey(username), ipKey(ip) }) {
			if (key != null && limiter.isLimited(key)) {
				Duration d = limiter.retryAfter(key);
				wait = d.compareTo(wait) > 0 ? d : wait;
			}
		}
		return wait.isZero() ? null : wait;
	}

	public void failed(String username, String ip) {
		limiter.hit(userKey(username));
		if (ip != null) {
			limiter.hit(ipKey(ip));
		}
	}

	public void succeeded(String username) {
		limiter.reset(userKey(username));
	}

	/** Xoá mọi bộ đếm (dùng trong test). */
	public void clear() {
		limiter.clear();
	}

	private static String userKey(String username) {
		return "u:" + username.trim().toLowerCase(Locale.ROOT);
	}

	private static String ipKey(String ip) {
		return ip == null ? null : "ip:" + ip;
	}
}
