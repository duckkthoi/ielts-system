package com.ltmtu.ielts.realtime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.realtime.RealtimeDtos.LiveSnapshot;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/** Trạng thái ban đầu của màn xem trực tiếp (BR-26) — docs/05-api.md mục A.6. */
@RestController
@RequestMapping("/api/teacher/parts/{partId}/students/{studentId}/live")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherLiveController {

	private final LiveDraftService liveDraftService;

	@GetMapping
	public ApiResponse<LiveSnapshot> snapshot(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long partId,
			@PathVariable Long studentId) {
		return ApiResponse.ok(liveDraftService.snapshot(me.id(), partId, studentId));
	}
}
