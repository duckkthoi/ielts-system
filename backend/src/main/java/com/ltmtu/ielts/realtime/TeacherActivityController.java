package com.ltmtu.ielts.realtime;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.realtime.dto.StudentActivityDtos.StudentActivityEvent;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/** Giáo viên xem lịch sử hoạt động của bài mình phụ trách. */
@RestController
@RequestMapping("/api/teacher/assignments")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherActivityController {

	private final StudentActivityService activityService;

	@GetMapping("/{id}/activity")
	public ApiResponse<List<StudentActivityEvent>> activity(@AuthenticationPrincipal AuthPrincipal me,
			@PathVariable Long id, @RequestParam(required = false) Long classId,
			@RequestParam(defaultValue = "100") int limit) {
		return ApiResponse.ok(activityService.teacherFeed(me.id(), id, classId, limit));
	}
}
