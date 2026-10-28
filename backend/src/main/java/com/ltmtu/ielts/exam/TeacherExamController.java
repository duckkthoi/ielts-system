package com.ltmtu.ielts.exam;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.exam.dto.ExamDtos.ExamMonitor;
import com.ltmtu.ielts.exam.dto.ExamDtos.ExamPush;
import com.ltmtu.ielts.exam.dto.ExamDtos.ExamTimeline;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/** Giám sát chế độ thi phía giáo viên — docs/05-api.md mục A.6. */
@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherExamController {

	private final TeacherExamService service;

	@GetMapping("/assignments/{id}/exam-monitor")
	public ApiResponse<ExamMonitor> monitor(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@RequestParam(required = false) Long classId) {
		return ApiResponse.ok(service.monitor(me.id(), id, classId));
	}

	@GetMapping("/assignments/{id}/exam-events")
	public ApiResponse<List<ExamPush>> recentEvents(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@RequestParam(required = false) Long classId) {
		return ApiResponse.ok(service.recentEvents(me.id(), id, classId));
	}

	@GetMapping("/exam-sessions/{id}/events")
	public ApiResponse<ExamTimeline> timeline(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(service.timeline(me.id(), id));
	}
}
