package com.ltmtu.ielts.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn tần suất kiểu cửa sổ trượt, giữ trong bộ nhớ (monolith một instance, không Redis — PLAN mục 7): tối đa
 * {@code max} lần trong {@code window} cho mỗi khoá. Class thuần, dùng {@link Clock} để test tua giờ được.
 */
public final class SlidingWindowLimiter {

	/** Quá nhiều khoá thì dọn hết khoá đã hết hạn, để bộ nhớ không phình mãi khi bị dò từ nhiều IP. */
	private static final int CLEANUP_THRESHOLD = 10_000;

	private final int max;
	private final Duration window;
	private final Clock clock;
	private final ConcurrentHashMap<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

	public SlidingWindowLimiter(int max, Duration window, Clock clock) {
		this.max = max;
		this.window = window;
		this.clock = clock;
	}

	/** Đã đủ {@code max} lần trong cửa sổ hiện tại. */
	public boolean isLimited(String key) {
		Deque<Instant> q = hits.get(key);
		if (q == null) {
			return false;
		}
		synchronized (q) {
			prune(q, clock.instant());
			return q.size() >= max;
		}
	}

	/** Còn bao lâu nữa thì lần cũ nhất ra khỏi cửa sổ (tối thiểu 1 giây). */
	public Duration retryAfter(String key) {
		Deque<Instant> q = hits.get(key);
		if (q == null) {
			return Duration.ZERO;
		}
		synchronized (q) {
			Instant now = clock.instant();
			prune(q, now);
			if (q.isEmpty()) {
				return Duration.ZERO;
			}
			Duration d = Duration.between(now, q.peekFirst().plus(window));
			return d.compareTo(Duration.ofSeconds(1)) < 0 ? Duration.ofSeconds(1) : d;
		}
	}

	public void hit(String key) {
		if (hits.size() > CLEANUP_THRESHOLD) {
			cleanup();
		}
		Deque<Instant> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
		synchronized (q) {
			Instant now = clock.instant();
			prune(q, now);
			q.addLast(now);
		}
	}

	public void reset(String key) {
		hits.remove(key);
	}

	/** Xoá mọi khoá (test, hoặc admin cần mở chặn ngay). */
	public void clear() {
		hits.clear();
	}

	private void cleanup() {
		Instant now = clock.instant();
		hits.entrySet().removeIf(e -> {
			synchronized (e.getValue()) {
				prune(e.getValue(), now);
				return e.getValue().isEmpty();
			}
		});
	}

	private void prune(Deque<Instant> q, Instant now) {
		Instant cutoff = now.minus(window);
		while (!q.isEmpty() && !q.peekFirst().isAfter(cutoff)) {
			q.pollFirst();
		}
	}
}
