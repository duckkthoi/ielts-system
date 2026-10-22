package com.ltmtu.ielts.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.dto.AuthDtos.AccountInfo;
import com.ltmtu.ielts.user.dto.AuthDtos.ChangePasswordRequest;
import com.ltmtu.ielts.user.dto.AuthDtos.LoginRequest;
import com.ltmtu.ielts.user.dto.AuthDtos.LoginResponse;
import com.ltmtu.ielts.user.dto.AuthDtos.RefreshRequest;
import com.ltmtu.ielts.user.dto.AuthDtos.TokenPair;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API xác thực — docs/05-api.md mục A.2. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest req) {
		return ApiResponse.ok(authService.login(body.username(), body.password(), ClientInfo.from(req)));
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenPair> refresh(@Valid @RequestBody RefreshRequest body) {
		return ApiResponse.ok(authService.refresh(body.refreshToken()));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest body) {
		authService.logout(body.refreshToken());
		return ApiResponse.message("Đã đăng xuất");
	}

	@GetMapping("/me")
	public ApiResponse<AccountInfo> me(@AuthenticationPrincipal AuthPrincipal me) {
		return ApiResponse.ok(authService.me(me));
	}

	@PostMapping("/change-password")
	public ApiResponse<AccountInfo> changePassword(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody ChangePasswordRequest body) {
		return ApiResponse.ok(authService.changePassword(me, body.currentPassword(), body.newPassword()),
				"Đổi mật khẩu thành công");
	}
}
