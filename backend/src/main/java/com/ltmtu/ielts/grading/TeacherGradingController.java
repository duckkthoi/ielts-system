package com.ltmtu.ielts.grading;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.grading.dto.GradingDtos.GradeDto;
import com.ltmtu.ielts.grading.dto.GradingDtos.GradingScreen;
import com.ltmtu.ielts.grading.dto.GradingDtos.QueueRow;
import com.ltmtu.ielts.grading.dto.GradingDtos.RevisionRequest;
import com.ltmtu.ielts.grading.dto.GradingDtos.SaveGradeRequest;
import com.ltmtu.ielts.grading.dto.GradingDtos.VersionSummary;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.submission.SubmissionStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Chấm bài của giáo viên — docs/05-api.md mục A.7 (UC-17, UC-18). */
@RestController
@RequestMapping("/api/teacher/submissions")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherGradingController {

	private final GradingService gradingService;

	@GetMapping
	public ApiResponse<List<QueueRow>> queue(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) SubmissionStatus status, @RequestParam(required = false) Long assignmentId,
			@RequestParam(required = false) Long partId, @RequestParam(required = false) Long classId,
			@RequestParam(required = false) String keyword, Pageable pageable) {
		return ApiResponse.page(gradingService.queue(me.id(), status, assignmentId, partId, classId, keyword, pageable));
	}

	@GetMapping("/{id}")
	public ApiResponse<GradingScreen> screen(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@RequestParam(required = false) Integer versionNo) {
		return ApiResponse.ok(gradingService.screen(me.id(), id, versionNo));
	}

	@GetMapping("/{id}/versions")
	public ApiResponse<List<VersionSummary>> versions(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(gradingService.versions(me.id(), id));
	}

	@PutMapping("/{id}/grade")
	public ApiResponse<GradeDto> saveGrade(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody SaveGradeRequest body) {
		return ApiResponse.ok(gradingService.saveGrade(me.id(), id, body), "Đã lưu nháp chấm");
	}

	@PostMapping("/{id}/return")
	public ApiResponse<GradingScreen> returnGrade(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(gradingService.returnGrade(me.id(), id), "Đã trả bài");
	}

	@PostMapping("/{id}/request-revision")
	public ApiResponse<GradingScreen> requestRevision(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody RevisionRequest body) {
		return ApiResponse.ok(gradingService.requestRevision(me.id(), id, body), "Đã yêu cầu sửa bài");
	}
}
