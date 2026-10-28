package com.ltmtu.ielts.realtime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.realtime.dto.StudentActivityDtos.RecordActivityRequest;
import com.ltmtu.ielts.realtime.dto.StudentActivityDtos.StudentActivityEvent;
import com.ltmtu.ielts.security.AuthPrincipal;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

/** Sinh viên gửi trạng thái vào/rời màn làm bài hoặc chuyển tab. */
@RestController
@RequestMapping("/api/student/assignments")
@PreAuthorize("hasRole('STUDENT')")
@Validated
@RequiredArgsConstructor
public class StudentActivityController {

	private final StudentActivityService activityService;

	@PostMapping("/{id}/activity")
	public ApiResponse<StudentActivityEvent> record(@AuthenticationPrincipal AuthPrincipal me,
			@PathVariable Long id, @Valid @RequestBody(required = false) RecordActivityRequest request) {
		return ApiResponse.ok(activityService.record(me.id(), id, request));
	}
}
