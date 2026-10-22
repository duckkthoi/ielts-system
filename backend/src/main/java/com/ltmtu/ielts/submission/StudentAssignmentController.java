package com.ltmtu.ielts.submission;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.submission.dto.StudentAssignmentDtos.AssignmentDetail;
import com.ltmtu.ielts.submission.dto.StudentAssignmentDtos.AssignmentList;
import com.ltmtu.ielts.submission.dto.StudentAssignmentDtos.Tab;

import lombok.RequiredArgsConstructor;

/** Bài được giao phía sinh viên — docs/05-api.md mục A.8 (UC-22). */
@RestController
@RequestMapping("/api/student/assignments")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentAssignmentController {

	private final StudentAssignmentService service;

	@GetMapping
	public ApiResponse<AssignmentList> list(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) Tab tab) {
		return ApiResponse.ok(service.list(me.id(), tab));
	}

	@GetMapping("/{id}")
	public ApiResponse<AssignmentDetail> detail(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(service.detail(me.id(), id));
	}
}
