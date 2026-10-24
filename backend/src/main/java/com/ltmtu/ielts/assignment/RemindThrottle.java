package com.ltmtu.ielts.assignment;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.ltmtu.ielts.common.SlidingWindowLimiter;
import com.ltmtu.ielts.config.AppProperties;

/**
 * Giới hạn nhắc thủ công (UC-16): mỗi (giáo viên, bài) tối đa N lần trong cửa sổ thời gian, để sinh viên không bị
 * dội thông báo + email khi giáo viên bấm "Nhắc nhở" liên tục.
 */
@Component
public class RemindThrottle {

	private final SlidingWindowLimiter limiter;

	public RemindThrottle(AppProperties props, Clock clock) {
		this.limiter = new SlidingWindowLimiter(props.rateLimit().remindMax(), props.rateLimit().remindWindow(), clock);
	}

	/** {@code null} nếu được nhắc; nếu không thì là thời gian phải chờ. */
	Duration blockedFor(Long teacherId, Long assignmentId) {
		String key = teacherId + ":" + assignmentId;
		return limiter.isLimited(key) ? limiter.retryAfter(key) : null;
	}

	void reminded(Long teacherId, Long assignmentId) {
		limiter.hit(teacherId + ":" + assignmentId);
	}

	/** Xoá mọi bộ đếm (dùng trong test). */
	public void clear() {
		limiter.clear();
	}
}
