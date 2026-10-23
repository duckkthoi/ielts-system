package com.ltmtu.ielts.submission;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.submission.dto.SubmissionDtos.DraftResponse;
import com.ltmtu.ielts.submission.dto.SubmissionDtos.SaveDraftRequest;
import com.ltmtu.ielts.submission.dto.SubmissionDtos.SaveDraftResponse;
import com.ltmtu.ielts.submission.dto.SubmissionDtos.SubmitRequest;
import com.ltmtu.ielts.submission.dto.SubmissionDtos.SubmitResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API làm bài của sinh viên — docs/05-api.md mục A.8. Lịch sử nộp kèm kết quả chấm ở {@code grading.StudentResultController}. */
@RestController
@RequestMapping("/api/student/parts/{partId}")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentSubmissionController {

	private final StudentSubmissionService service;

	@GetMapping("/draft")
	public ApiResponse<DraftResponse> draft(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long partId) {
		return ApiResponse.ok(service.draft(me.id(), partId));
	}

	@PutMapping("/draft")
	public ApiResponse<SaveDraftResponse> saveDraft(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long partId,
			@Valid @RequestBody SaveDraftRequest body) {
		return ApiResponse.ok(service.saveDraft(me.id(), partId, body.content(), body.draftVersion()));
	}

	@PostMapping("/submit")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SubmitResponse> submit(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long partId,
			@Valid @RequestBody SubmitRequest body) {
		return ApiResponse.ok(service.submit(me.id(), partId, body.content(), body.expectedVersion()), "Đã nộp bài");
	}
}
