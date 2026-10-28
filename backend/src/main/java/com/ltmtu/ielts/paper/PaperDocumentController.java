package com.ltmtu.ielts.paper;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.DocumentState;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.SnapshotRequest;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.UpdatesAfter;
import com.ltmtu.ielts.security.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Tài liệu paper qua REST — docs/05-api.md mục A.10. Gửi update đi qua STOMP ({@link PaperSocketController}). */
@RestController
@RequiredArgsConstructor
public class PaperDocumentController {

	private final PaperDocumentService service;

	/** Sinh viên mở tài liệu của nhóm mình cho một giai đoạn (tạo nếu chưa có). */
	@GetMapping("/api/student/parts/{partId}/document")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<DocumentState> open(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long partId) {
		return ApiResponse.ok(service.open(me.id(), partId));
	}

	/** Trạng thái đầy đủ — thành viên nhóm hoặc giáo viên chủ bài (chỉ đọc). */
	@GetMapping("/api/papers/docs/{id}")
	@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
	public ApiResponse<DocumentState> state(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(service.state(me, id));
	}

	@GetMapping("/api/papers/docs/{id}/updates")
	@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
	public ApiResponse<UpdatesAfter> updates(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@RequestParam(defaultValue = "0") long afterSeq) {
		return ApiResponse.ok(service.updatesAfter(me, id, afterSeq));
	}

	@PutMapping("/api/papers/docs/{id}/snapshot")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<DocumentState> snapshot(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody SnapshotRequest body) {
		return ApiResponse.ok(service.saveSnapshot(me.id(), id, body));
	}
}
