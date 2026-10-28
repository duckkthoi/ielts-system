package com.ltmtu.ielts.paper.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO nhóm viết paper — docs/05-api.md mục A.10. */
public final class PaperGroupDtos {

	private PaperGroupDtos() {
	}

	// ─── Request ──────────────────────────────────────────────────────────────

	/** Giáo viên tạo / sửa nhóm: {@code studentIds} là toàn bộ thành viên (PUT = thay toàn bộ). */
	public record GroupRequest(
			@NotBlank(message = "Vui lòng nhập tên nhóm") @Size(max = 100, message = "Tên nhóm tối đa 100 ký tự") String name,
			Set<Long> studentIds) {
	}

	/** Chia ngẫu nhiên những người chưa có nhóm; {@code classId} = chỉ trong một lớp. */
	public record AutoGroupRequest(Long classId) {
	}

	/** Sinh viên tự lập nhóm. */
	public record CreateGroupRequest(
			@NotBlank(message = "Vui lòng nhập tên nhóm") @Size(max = 100, message = "Tên nhóm tối đa 100 ký tự") String name) {
	}

	// ─── Response ─────────────────────────────────────────────────────────────

	public record StudentRef(Long id, String studentCode, String fullName) {
	}

	public record MemberDto(Long studentId, String studentCode, String fullName, Instant joinedAt) {
	}

	/** {@code studentMade}: sinh viên tự lập (tự xoá khi không còn ai). */
	public record GroupDto(Long id, String name, boolean studentMade, List<MemberDto> members) {
	}

	/**
	 * Màn chia nhóm của giáo viên. {@code lockAt}: hạn chốt nhóm thực tế (không đặt thì là hạn nộp);
	 * {@code ungrouped}: người nhận bài chưa có nhóm.
	 */
	public record TeacherGroups(Long assignmentId, int maxGroupSize, Instant lockAt, boolean locked,
			List<GroupDto> groups, List<StudentRef> ungrouped) {
	}

	/** Phía sinh viên: {@code canChange} = còn tự tạo / vào / rời nhóm được. */
	public record StudentGroups(Long assignmentId, Long myGroupId, boolean canChange, int maxGroupSize, Instant lockAt,
			List<GroupDto> groups) {
	}
}
