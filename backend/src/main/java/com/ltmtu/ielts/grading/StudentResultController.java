package com.ltmtu.ielts.grading;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.AssignmentResult;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.VersionDetail;
import com.ltmtu.ielts.grading.dto.StudentResultDtos.VersionSummary;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * Lịch sử nộp và kết quả chấm phía sinh viên — docs/05-api.md mục A.8. Đặt ở module chấm bài để phụ thuộc chỉ đi một
 * chiều (chấm bài → bài nộp).
 */
@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentResultController {

	private final StudentResultService resultService;

	@GetMapping("/parts/{partId}/versions")
	public ApiResponse<List<VersionSummary>> versions(@AuthenticationPrincipal AuthPrincipal me,
			@PathVariable Long partId) {
		return ApiResponse.ok(resultService.versions(me.id(), partId));
	}

	@GetMapping("/parts/{partId}/versions/{versionNo}")
	public ApiResponse<VersionDetail> version(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long partId,
			@PathVariable int versionNo) {
		return ApiResponse.ok(resultService.version(me.id(), partId, versionNo));
	}

	@GetMapping("/assignments/{assignmentId}/result")
	public ApiResponse<AssignmentResult> assignmentResult(@AuthenticationPrincipal AuthPrincipal me,
			@PathVariable Long assignmentId) {
		return ApiResponse.ok(resultService.assignmentResult(me.id(), assignmentId));
	}
}
