package com.ltmtu.ielts.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** Đồng hồ điều khiển được trong test: tua nhanh để kiểm tra hết hạn token, hạn nộp, nhắc hạn. */
public class MutableClock extends Clock {

	private final AtomicReference<Instant> now = new AtomicReference<>(nowMicros());

	public void reset() {
		now.set(nowMicros());
	}

	/** Cùng độ chính xác với DB (micro giây) như Clock của ứng dụng — xem TimeConfig. */
	private static Instant nowMicros() {
		return Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
	}

	public void advance(Duration duration) {
		now.updateAndGet(t -> t.plus(duration));
	}

	public void set(Instant instant) {
		now.set(instant);
	}

	@Override
	public Instant instant() {
		return now.get();
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}
}
