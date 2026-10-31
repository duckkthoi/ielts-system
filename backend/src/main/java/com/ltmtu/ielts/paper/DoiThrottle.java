package com.ltmtu.ielts.paper;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.ltmtu.ielts.common.SlidingWindowLimiter;
import com.ltmtu.ielts.config.AppProperties;

/** Giới hạn số lần tra DOI mỗi phút của một người (dịch vụ ngoài, không để một người gọi dồn). */
@Component
public class DoiThrottle {

	private final SlidingWindowLimiter limiter;

	public DoiThrottle(AppProperties props, Clock clock) {
		this.limiter = new SlidingWindowLimiter(props.paper().doiLookupsPerMinute(), Duration.ofMinutes(1), clock);
	}

	/** {@code null} nếu được tra; nếu không thì là thời gian phải chờ. Được tra thì tính luôn vào bộ đếm. */
	Duration tryLookup(Long userId) {
		String key = String.valueOf(userId);
		return limiter.tryAcquire(key) ? null : limiter.retryAfter(key);
	}

	/** Xoá mọi bộ đếm (dùng trong test). */
	public void clear() {
		limiter.clear();
	}
}
