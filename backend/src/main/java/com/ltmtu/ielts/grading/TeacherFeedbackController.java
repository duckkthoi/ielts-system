package com.ltmtu.ielts.grading;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.grading.GradingEnums.CriterionGroup;
import com.ltmtu.ielts.grading.dto.FeedbackDtos.CommentBankDto;
import com.ltmtu.ielts.grading.dto.FeedbackDtos.CommentBankRequest;
import com.ltmtu.ielts.grading.dto.FeedbackDtos.ErrorCodeDto;
import com.ltmtu.ielts.grading.dto.FeedbackDtos.ErrorCodeRequest;
import com.ltmtu.ielts.security.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Mã lỗi và ngân hàng nhận xét của giáo viên — docs/05-api.md mục A.7. */
@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherFeedbackController {

	private final FeedbackLibraryService feedbackLibraryService;

	@GetMapping("/error-codes")
	public ApiResponse<List<ErrorCodeDto>> errorCodes(@AuthenticationPrincipal AuthPrincipal me) {
		return ApiResponse.ok(feedbackLibraryService.errorCodes(me.id()));
	}

	@PostMapping("/error-codes")
	public ApiResponse<ErrorCodeDto> createErrorCode(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody ErrorCodeRequest body) {
		return ApiResponse.ok(feedbackLibraryService.createErrorCode(me.id(), body), "Đã thêm mã lỗi");
	}

	@PutMapping("/error-codes/{id}")
	public ApiResponse<ErrorCodeDto> updateErrorCode(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody ErrorCodeRequest body) {
		return ApiResponse.ok(feedbackLibraryService.updateErrorCode(me.id(), id, body), "Đã cập nhật mã lỗi");
	}

	@DeleteMapping("/error-codes/{id}")
	public ApiResponse<Void> deleteErrorCode(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		feedbackLibraryService.deleteErrorCode(me.id(), id);
		return ApiResponse.message("Đã xoá mã lỗi");
	}

	@GetMapping("/comment-bank")
	public ApiResponse<List<CommentBankDto>> comments(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) CriterionGroup criterion, @RequestParam(required = false) String keyword) {
		return ApiResponse.ok(feedbackLibraryService.comments(me.id(), criterion, keyword));
	}

	@PostMapping("/comment-bank")
	public ApiResponse<CommentBankDto> createComment(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody CommentBankRequest body) {
		return ApiResponse.ok(feedbackLibraryService.createComment(me.id(), body), "Đã thêm nhận xét");
	}

	@PutMapping("/comment-bank/{id}")
	public ApiResponse<CommentBankDto> updateComment(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody CommentBankRequest body) {
		return ApiResponse.ok(feedbackLibraryService.updateComment(me.id(), id, body), "Đã cập nhật nhận xét");
	}

	@DeleteMapping("/comment-bank/{id}")
	public ApiResponse<Void> deleteComment(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		feedbackLibraryService.deleteComment(me.id(), id);
		return ApiResponse.message("Đã xoá nhận xét");
	}

	@PostMapping("/comment-bank/{id}/use")
	public ApiResponse<CommentBankDto> markUsed(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(feedbackLibraryService.markUsed(me.id(), id));
	}
}
