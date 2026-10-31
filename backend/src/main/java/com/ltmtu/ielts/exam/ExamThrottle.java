package com.ltmtu.ielts.exam;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.ltmtu.ielts.common.SlidingWindowLimiter;
import com.ltmtu.ielts.config.AppProperties;

/**
 * Giới hạn tần suất của chế độ thi, trong bộ nhớ:
 * <ul>
 * <li>sự kiện client gửi: tối đa N / phút mỗi phiên — client lỗi hoặc cố tình gửi dồn không làm đầy bảng;</li>
 * <li>thông báo chuyển tab / dán cho giáo viên: tối đa 1 lần / {@code alertWindow} mỗi phiên (BR-25) — sự kiện vẫn
 * được ghi và hiện trên màn giám sát, chỉ chuông thông báo là không bị dội.</li>
 * </ul>
 */
@Component
public class ExamThrottle {

	private final SlidingWindowLimiter events;
	private final SlidingWindowLimiter alerts;

	public ExamThrottle(AppProperties props, Clock clock) {
		this.events = new SlidingWindowLimiter(props.exam().maxEventsPerMinute(), Duration.ofMinutes(1), clock);
		this.alerts = new SlidingWindowLimiter(1, props.exam().alertWindow(), clock);
	}

	/** Nhận thêm một sự kiện của phiên được không; được thì tính luôn vào bộ đếm. */
	boolean acceptEvent(Long sessionId) {
		return tryHit(events, sessionId);
	}

	/** Có báo giáo viên lần này không; có thì tính luôn vào bộ đếm. */
	boolean acceptAlert(Long sessionId) {
		return tryHit(alerts, sessionId);
	}

	private static boolean tryHit(SlidingWindowLimiter limiter, Long sessionId) {
		return limiter.tryAcquire(String.valueOf(sessionId));
	}

	/** Xoá mọi bộ đếm (dùng trong test). */
	public void clear() {
		events.clear();
		alerts.clear();
	}
}
