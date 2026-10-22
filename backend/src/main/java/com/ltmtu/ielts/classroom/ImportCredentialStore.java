package com.ltmtu.ielts.classroom;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Giữ tạm danh sách tài khoản + mật khẩu vừa tạo khi import (SD-03).
 * <ul>
 * <li>Chỉ nằm trong <b>bộ nhớ</b>, không bao giờ ghi DB hay log (mật khẩu dạng rõ).</li>
 * <li>Lấy ra được <b>một lần</b> ({@link #take}), và chỉ bởi đúng giáo viên đã import.</li>
 * <li>Tự huỷ sau {@link #TTL}.</li>
 * </ul>
 * Khởi động lại server thì mất — chấp nhận được: giáo viên đặt lại mật khẩu cho sinh viên cần.
 */
@Component
@RequiredArgsConstructor
public class ImportCredentialStore {

	public static final Duration TTL = Duration.ofMinutes(15);

	public record Credential(String studentCode, String fullName, String email, String temporaryPassword) {
	}

	public record Entry(Long teacherId, String classroomCode, List<Credential> credentials, Instant expiresAt) {
	}

	private final Map<String, Entry> entries = new ConcurrentHashMap<>();
	private final Clock clock;

	/** Lưu và trả về token để tải file. */
	public Issued put(Long teacherId, String classroomCode, List<Credential> credentials) {
		String token = UUID.randomUUID().toString();
		Instant expiresAt = clock.instant().plus(TTL);
		entries.put(token, new Entry(teacherId, classroomCode, List.copyOf(credentials), expiresAt));
		return new Issued(token, expiresAt);
	}

	/** Lấy ra và xoá. Sai giáo viên, đã lấy, hoặc hết hạn → rỗng. */
	public Optional<Entry> take(String token, Long teacherId) {
		Entry entry = entries.get(token);
		if (entry == null || !entry.teacherId().equals(teacherId)) {
			return Optional.empty();
		}
		if (!entries.remove(token, entry) || !entry.expiresAt().isAfter(clock.instant())) {
			return Optional.empty();
		}
		return Optional.of(entry);
	}

	@Scheduled(fixedDelayString = "PT1M")
	void purgeExpired() {
		Instant now = clock.instant();
		entries.values().removeIf(e -> !e.expiresAt().isAfter(now));
	}

	public record Issued(String token, Instant expiresAt) {
	}
}
