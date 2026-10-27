package com.ltmtu.ielts.realtime.dto;

import java.time.Instant;

import com.ltmtu.ielts.realtime.StudentActivityType;

import jakarta.validation.constraints.NotNull;

/** Hoạt động của sinh viên trong màn làm bài (FR-54). */
public final class StudentActivityDtos {

	private StudentActivityDtos() {
	}

	public record RecordActivityRequest(@NotNull(message = "Thiếu loại hoạt động cần ghi nhận") StudentActivityType type) {
	}

	public record StudentActivityEvent(Long id, Long studentId, String studentCode, String fullName,
			StudentActivityType type, Instant occurredAt) {
	}
}
