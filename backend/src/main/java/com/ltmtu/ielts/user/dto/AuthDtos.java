package com.ltmtu.ielts.user.dto;

import java.time.Instant;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO của nhóm API xác thực — docs/05-api.md mục A.2. */
public final class AuthDtos {

	private AuthDtos() {
	}

	public record LoginRequest(
			@NotBlank(message = "Vui lòng nhập tên đăng nhập") @Size(max = 50) String username,
			@NotBlank(message = "Vui lòng nhập mật khẩu") @Size(max = 100) String password) {
	}

	public record RefreshRequest(@NotBlank(message = "Thiếu refresh token") String refreshToken) {
	}

	public record ChangePasswordRequest(
			@NotBlank(message = "Vui lòng nhập mật khẩu hiện tại") String currentPassword,
			@NotBlank(message = "Vui lòng nhập mật khẩu mới")
			@Size(min = 8, max = 100, message = "Mật khẩu mới phải từ 8 đến 100 ký tự")
			@Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$", message = "Mật khẩu mới phải có cả chữ và số")
			String newPassword) {
	}

	public record TokenPair(String accessToken, String refreshToken, Instant accessTokenExpiresAt) {
	}

	public record LoginResponse(String accessToken, String refreshToken, Instant accessTokenExpiresAt,
			AccountInfo user) {
	}

	/** Thông tin tài khoản trả cho client (khớp kiểu AccountInfo ở frontend). */
	public record AccountInfo(Long id, String username, String fullName, String email, Role role,
			String studentCode, boolean mustChangePassword) {

		public static AccountInfo from(User u) {
			return new AccountInfo(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(),
					u.getStudentCode(), u.isMustChangePassword());
		}
	}
}
