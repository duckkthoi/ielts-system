package com.ltmtu.ielts.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ltmtu.ielts.support.MutableClock;

/** Cửa sổ trượt của {@link SlidingWindowLimiter}. */
class SlidingWindowLimiterTest {

	private final MutableClock clock = new MutableClock();
	private final SlidingWindowLimiter limiter = new SlidingWindowLimiter(3, Duration.ofMinutes(5), clock);

	@Test
	@DisplayName("Đủ 3 lần trong 5 phút thì bị chặn; lần cũ nhất ra khỏi cửa sổ thì hết chặn; khoá khác không ảnh hưởng")
	void slidingWindow() {
		limiter.hit("a");
		clock.advance(Duration.ofMinutes(1));
		limiter.hit("a");
		limiter.hit("a");
		assertThat(limiter.isLimited("a")).isTrue();
		assertThat(limiter.isLimited("b")).isFalse();
		assertThat(limiter.retryAfter("a")).isEqualTo(Duration.ofMinutes(4));

		clock.advance(Duration.ofMinutes(4));
		assertThat(limiter.isLimited("a")).isFalse();
		assertThat(limiter.retryAfter("a")).isEqualTo(Duration.ofMinutes(1));
	}

	@Test
	@DisplayName("reset xoá bộ đếm của một khoá, clear xoá tất cả")
	void resetAndClear() {
		for (int i = 0; i < 3; i++) {
			limiter.hit("a");
			limiter.hit("b");
		}
		limiter.reset("a");
		assertThat(limiter.isLimited("a")).isFalse();
		assertThat(limiter.isLimited("b")).isTrue();
		limiter.clear();
		assertThat(limiter.isLimited("b")).isFalse();
	}
}
