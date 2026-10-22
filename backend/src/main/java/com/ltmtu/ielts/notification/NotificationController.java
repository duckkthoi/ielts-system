package com.ltmtu.ielts.notification;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/** Thông báo của người dùng hiện tại (mọi vai trò) — docs/05-api.md mục A.9. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	/** {@code after}: chỉ lấy thông báo tạo sau thời điểm này — dùng khi kết nối lại WebSocket (SD-09). */
	@GetMapping
	public ApiResponse<List<NotificationDto>> list(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(defaultValue = "false") boolean unreadOnly, @RequestParam(required = false) Instant after,
			Pageable pageable) {
		return ApiResponse.page(notificationService.list(me.id(), unreadOnly, after, pageable));
	}

	@GetMapping("/unread-count")
	public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal AuthPrincipal me) {
		return ApiResponse.ok(Map.of("count", notificationService.unreadCount(me.id())));
	}

	@PostMapping("/{id}/read")
	public ApiResponse<NotificationDto> markRead(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(notificationService.markRead(me.id(), id));
	}

	@PostMapping("/read-all")
	public ApiResponse<Map<String, Integer>> markAllRead(@AuthenticationPrincipal AuthPrincipal me) {
		return ApiResponse.ok(Map.of("updated", notificationService.markAllRead(me.id())));
	}
}
