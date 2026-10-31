package com.ltmtu.ielts.paper;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.CommentDto;
import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.CreateRequest;
import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.ResolveRequest;
import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.UpdateRequest;
import com.ltmtu.ielts.security.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Nhận xét trực tiếp trên tài liệu Paper, tách khỏi điểm chấm bản nộp — docs/05 mục A.10. */
@RestController
@RequiredArgsConstructor
public class PaperLiveCommentController {

	private final PaperLiveCommentService service;

	@GetMapping("/api/papers/docs/{documentId}/comments")
	@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
	public ApiResponse<List<CommentDto>> list(@AuthenticationPrincipal AuthPrincipal me,
			@PathVariable Long documentId) {
		return ApiResponse.ok(service.list(me, documentId));
	}

	@PostMapping("/api/teacher/papers/docs/{documentId}/comments")
	@PreAuthorize("hasRole('TEACHER')")
	public ApiResponse<CommentDto> add(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long documentId,
			@Valid @RequestBody CreateRequest body) {
		return ApiResponse.ok(service.add(me, documentId, body), "Đã thêm nhận xét");
	}

	@PutMapping("/api/teacher/papers/docs/{documentId}/comments/{id}")
	@PreAuthorize("hasRole('TEACHER')")
	public ApiResponse<CommentDto> update(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long documentId,
			@PathVariable Long id, @Valid @RequestBody UpdateRequest body) {
		return ApiResponse.ok(service.update(me, documentId, id, body), "Đã sửa nhận xét");
	}

	@DeleteMapping("/api/teacher/papers/docs/{documentId}/comments/{id}")
	@PreAuthorize("hasRole('TEACHER')")
	public ApiResponse<Void> delete(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long documentId,
			@PathVariable Long id) {
		service.delete(me, documentId, id);
		return ApiResponse.message("Đã xoá nhận xét");
	}

	@PutMapping("/api/papers/docs/{documentId}/comments/{id}/resolve")
	@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
	public ApiResponse<CommentDto> resolve(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long documentId,
			@PathVariable Long id, @RequestBody ResolveRequest body) {
		return ApiResponse.ok(service.resolve(me, documentId, id, body.resolved()),
				body.resolved() ? "Đã đánh dấu đã xử lý" : "Đã mở lại nhận xét");
	}
}
