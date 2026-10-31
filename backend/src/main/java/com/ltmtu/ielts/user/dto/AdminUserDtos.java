package com.ltmtu.ielts.user.dto;

import java.time.Instant;
import java.util.List;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO của nhóm API quản trị — docs/05-api.md mục A.3. */
public final class AdminUserDtos {

	private AdminUserDtos() {
	}

	public record CreateTeacherRequest(
			@NotBlank(message = "Vui lòng nhập tên đăng nhập")
			@Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự")
			@Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Tên đăng nhập chỉ gồm chữ không dấu, số và . _ -")
			String username,
			@NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 150) String fullName,
			@Email(message = "Email không hợp lệ") @Size(max = 150) String email) {
	}

	public record UpdateTeacherRequest(
			@NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 150) String fullName,
			@Email(message = "Email không hợp lệ") @Size(max = 150) String email) {
	}

	/**
	 * Một dòng trong danh sách người dùng / giáo viên.
	 * {@code classCount}: số lớp phụ trách — chỉ có với giáo viên, còn lại {@code null}.
	 */
	public record UserSummary(Long id, String username, String fullName, String email, Role role,
			String studentCode, boolean active, boolean mustChangePassword, Instant lastLoginAt, Instant createdAt,
			Long classCount) {

		public static UserSummary from(User u) {
			return new UserSummary(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(),
					u.getStudentCode(), u.isActive(), u.isMustChangePassword(), u.getLastLoginAt(), u.getCreatedAt(),
					null);
		}

		public UserSummary withClassCount(Long count) {
			return new UserSummary(id, username, fullName, email, role, studentCode, active, mustChangePassword,
					lastLoginAt, createdAt, count);
		}
	}

	public record UserDetail(UserSummary user, long activeSessions) {
	}

	/** Mật khẩu tạm chỉ trả về đúng một lần trong response này. */
	public record CreatedTeacher(UserSummary teacher, String temporaryPassword) {
	}

	public record TemporaryPassword(String temporaryPassword) {
	}

	// ─── Import giáo viên từ file (UC-05b) ────────────────────────────────────

	public record TeacherImportError(int row, String username, String reason) {
	}

	/** Tài khoản vừa tạo. Mật khẩu tạm chỉ có trong response này: không ghi DB, không ghi log (BR-02). */
	public record ImportedTeacher(Long id, String username, String fullName, String email, String temporaryPassword) {
	}

	/** {@code skipped}: tên đăng nhập đã là giáo viên (không đổi gì). */
	public record TeacherImportReport(int totalRows, int created, int skipped, List<TeacherImportError> errors,
			List<ImportedTeacher> accounts) {
	}
}
