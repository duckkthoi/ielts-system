package com.ltmtu.ielts.classroom.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO của nhóm API lớp học — docs/05-api.md mục A.4. */
public final class ClassroomDtos {

	private ClassroomDtos() {
	}

	public record ClassroomRequest(
			@NotBlank(message = "Vui lòng nhập mã lớp")
			@Size(min = 2, max = 30, message = "Mã lớp từ 2 đến 30 ký tự")
			@Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Mã lớp chỉ gồm chữ không dấu, số và . _ -")
			String code,
			@NotBlank(message = "Vui lòng nhập tên lớp") @Size(max = 150) String name,
			@Size(max = 2000) String description) {
	}

	/** {@code openAssignmentCount}: số bài đang mở (PUBLISHED) được giao cho cả lớp. */
	public record ClassroomSummary(Long id, String code, String name, String description, long memberCount,
			long openAssignmentCount, Instant createdAt) {
	}

	public record AddMembersRequest(
			@NotEmpty(message = "Chưa chọn sinh viên nào")
			@Size(max = 500, message = "Tối đa 500 sinh viên mỗi lần")
			List<@NotBlank String> studentCodes) {
	}

	/** Kết quả thêm sinh viên đã có tài khoản vào lớp. */
	public record AddMembersResult(int added, int alreadyMember, List<String> notFound) {
	}

	public record MemberDto(Long studentId, String studentCode, String fullName, String email, boolean active,
			boolean mustChangePassword, Instant joinedAt) {
	}

	/** Kết quả tìm sinh viên để giao bài cá nhân. */
	public record StudentOption(Long id, String studentCode, String fullName) {
	}
}
