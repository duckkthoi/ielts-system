package com.ltmtu.ielts.user;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.dto.AdminSessionDtos.AffectedCount;
import com.ltmtu.ielts.user.dto.AdminSessionDtos.SessionRow;

import lombok.RequiredArgsConstructor;

/** API phiên đăng nhập cho admin (UC-08) — docs/05-api.md mục A.3. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSessionController {

	private final AdminSessionService sessionService;

	@GetMapping("/sessions")
	public ApiResponse<List<SessionRow>> sessions(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) String keyword, @RequestParam(required = false) Role role,
			@RequestParam(required = false) SessionRules.Status status, @RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to, Pageable pageable) {
		return ApiResponse.page(sessionService.search(me, keyword, role, status, from, to, pageable));
	}

	@DeleteMapping("/sessions/{id}")
	public ApiResponse<SessionRow> revoke(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(sessionService.revoke(me, id), "Đã thu hồi phiên đăng nhập");
	}

	@PostMapping("/users/{id}/sessions/revoke")
	public ApiResponse<AffectedCount> revokeAll(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		AffectedCount result = sessionService.revokeAllOfUser(me, id);
		return ApiResponse.ok(result, "Đã thu hồi " + result.count() + " phiên của tài khoản");
	}

	@PostMapping("/sessions/cleanup")
	public ApiResponse<AffectedCount> cleanup(@AuthenticationPrincipal AuthPrincipal me) {
		AffectedCount result = sessionService.cleanupExpired(me);
		return ApiResponse.ok(result, "Đã dọn " + result.count() + " phiên hết hạn");
	}
}
