package com.ltmtu.ielts.assignment;

import org.springframework.data.domain.Pageable;
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

import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.assignment.dto.QuestionBankDtos.QuestionBankDetail;
import com.ltmtu.ielts.assignment.dto.QuestionBankDtos.QuestionBankRequest;
import com.ltmtu.ielts.assignment.dto.QuestionBankDtos.QuestionBankSummary;
import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API ngân hàng đề của giáo viên — UC-20 / docs/05 A.5. */
@RestController
@RequestMapping("/api/teacher/question-bank")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherQuestionBankController {

	private final QuestionBankService service;

	@GetMapping
	public ApiResponse<java.util.List<QuestionBankSummary>> search(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) TaskType taskType, @RequestParam(required = false) String keyword,
			@RequestParam(required = false) String tag, Pageable pageable) {
		return ApiResponse.page(service.search(me.id(), taskType, keyword, tag, pageable));
	}

	@PostMapping
	public ApiResponse<QuestionBankDetail> create(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody QuestionBankRequest body) {
		return ApiResponse.ok(service.create(me.id(), body), "Đã lưu đề vào ngân hàng");
	}

	@GetMapping("/{id}")
	public ApiResponse<QuestionBankDetail> detail(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(service.detail(me.id(), id));
	}

	@PutMapping("/{id}")
	public ApiResponse<QuestionBankDetail> replace(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody QuestionBankRequest body) {
		return ApiResponse.ok(service.replace(me.id(), id, body), "Đã cập nhật đề trong ngân hàng");
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		service.delete(me.id(), id);
		return ApiResponse.message("Đã xoá đề khỏi ngân hàng");
	}
}
