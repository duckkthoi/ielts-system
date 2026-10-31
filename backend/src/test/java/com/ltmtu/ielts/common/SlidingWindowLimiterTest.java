package com.ltmtu.ielts.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

	@Test
	@DisplayName("tryAcquire nguyên tử: nhiều luồng gọi cùng lúc cũng chỉ được đúng max lần")
	void tryAcquireConcurrent() throws InterruptedException {
		SlidingWindowLimiter big = new SlidingWindowLimiter(120, Duration.ofMinutes(1), clock);
		AtomicInteger ok = new AtomicInteger();
		ExecutorService pool = Executors.newFixedThreadPool(8);
		for (int i = 0; i < 400; i++) {
			pool.execute(() -> {
				if (big.tryAcquire("k")) {
					ok.incrementAndGet();
				}
			});
		}
		pool.shutdown();
		assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		assertThat(ok.get()).isEqualTo(120);
		assertThat(big.tryAcquire("k")).isFalse();
		assertThat(big.tryAcquire("khác")).isTrue();
	}
}
