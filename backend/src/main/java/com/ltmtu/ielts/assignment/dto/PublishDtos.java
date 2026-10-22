package com.ltmtu.ielts.assignment.dto;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** DTO phát hành / giao bài — docs/05-api.md mục A.5 (UC-14). */
public final class PublishDtos {

	private PublishDtos() {
	}

	/** Lớp và/hoặc từng sinh viên nhận bài. Hai danh sách có thể trùng người — server tự loại trùng. */
	public record TargetsRequest(
			@Size(max = 50, message = "Tối đa 50 lớp mỗi lần") Set<Long> classIds,
			@Size(max = 500, message = "Tối đa 500 sinh viên mỗi lần") Set<Long> studentIds) {
	}

	public record PublishResult(int recipientCount) {
	}

	public record TargetsAdded(int newRecipients, int recipientCount) {
	}

	public record TargetClass(Long id, String code, String name, long memberCount) {
	}

	public record TargetStudent(Long id, String studentCode, String fullName) {
	}

	/** Nhắc thủ công (UC-16): {@code partId} null = nhắc cả bài. */
	public record RemindRequest(
			@NotEmpty(message = "Chưa chọn sinh viên cần nhắc")
			@Size(max = 500, message = "Tối đa 500 sinh viên mỗi lần") Set<Long> studentIds,
			Long partId) {
	}

	public record RemindResult(int sent) {
	}

	/** Danh sách đã giao + số người nhận thực tế (lớp ∪ cá nhân, đã loại trùng). */
	public record Targets(List<TargetClass> classes, List<TargetStudent> students, int recipientCount) {
	}
}
