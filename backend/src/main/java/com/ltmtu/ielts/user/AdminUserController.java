package com.ltmtu.ielts.user;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.dto.AdminUserDtos.CreateTeacherRequest;
import com.ltmtu.ielts.user.dto.AdminUserDtos.CreatedTeacher;
import com.ltmtu.ielts.user.dto.AdminUserDtos.TemporaryPassword;
import com.ltmtu.ielts.user.dto.AdminUserDtos.UpdateTeacherRequest;
import com.ltmtu.ielts.user.dto.AdminUserDtos.UserDetail;
import com.ltmtu.ielts.user.dto.AdminUserDtos.UserSummary;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API quản trị người dùng — docs/05-api.md mục A.3. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

	private final AdminUserService adminUserService;

	@GetMapping("/teachers")
	public ApiResponse<List<UserSummary>> teachers(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Boolean active, Pageable pageable) {
		return ApiResponse.page(adminUserService.search(keyword, Role.TEACHER, active, pageable));
	}

	@PostMapping("/teachers")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<CreatedTeacher> createTeacher(@Valid @RequestBody CreateTeacherRequest body) {
		return ApiResponse.ok(adminUserService.createTeacher(body), "Đã tạo tài khoản giáo viên");
	}

	@PutMapping("/teachers/{id}")
	public ApiResponse<UserSummary> updateTeacher(@PathVariable Long id, @Valid @RequestBody UpdateTeacherRequest body) {
		return ApiResponse.ok(adminUserService.updateTeacher(id, body));
	}

	@GetMapping("/users")
	public ApiResponse<List<UserSummary>> users(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Role role, @RequestParam(required = false) Boolean active,
			Pageable pageable) {
		return ApiResponse.page(adminUserService.search(keyword, role, active, pageable));
	}

	@GetMapping("/users/{id}")
	public ApiResponse<UserDetail> user(@PathVariable Long id) {
		return ApiResponse.ok(adminUserService.detail(id));
	}

	@PostMapping("/users/{id}/lock")
	public ApiResponse<UserSummary> lock(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(adminUserService.lock(me, id), "Đã khoá tài khoản");
	}

	@PostMapping("/users/{id}/unlock")
	public ApiResponse<UserSummary> unlock(@PathVariable Long id) {
		return ApiResponse.ok(adminUserService.unlock(id), "Đã mở khoá tài khoản");
	}

	@PostMapping("/users/{id}/reset-password")
	public ApiResponse<TemporaryPassword> resetPassword(@AuthenticationPrincipal AuthPrincipal me,
			@PathVariable Long id) {
		return ApiResponse.ok(adminUserService.resetPassword(me, id), "Đã đặt lại mật khẩu");
	}
}
