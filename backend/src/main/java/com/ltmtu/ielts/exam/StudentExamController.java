package com.ltmtu.ielts.exam;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.exam.dto.ExamDtos.ExamSessionDto;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/** Chế độ thi phía sinh viên — docs/05-api.md mục A.8. */
@RestController
@RequestMapping("/api/student/parts/{partId}/exam")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentExamController {

	private final StudentExamService service;

	@GetMapping
	public ApiResponse<ExamSessionDto> current(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long partId) {
		return ApiResponse.ok(service.current(me.id(), partId));
	}

	@PostMapping("/start")
	public ApiResponse<ExamSessionDto> start(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long partId) {
		return ApiResponse.ok(service.start(me.id(), partId));
	}
}
