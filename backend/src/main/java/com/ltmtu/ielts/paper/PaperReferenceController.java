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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.paper.dto.PaperReferenceDtos.BatchRequest;
import com.ltmtu.ielts.paper.dto.PaperReferenceDtos.BatchResult;
import com.ltmtu.ielts.paper.dto.PaperReferenceDtos.DoiLookup;
import com.ltmtu.ielts.paper.dto.PaperReferenceDtos.ReferenceDto;
import com.ltmtu.ielts.paper.dto.PaperReferenceDtos.ReferenceRequest;
import com.ltmtu.ielts.security.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Thư viện trích dẫn của nhóm và tra DOI — docs/05-api.md mục A.10 (B24). */
@RestController
@RequiredArgsConstructor
public class PaperReferenceController {

	private final PaperReferenceService service;

	/** Thành viên nhóm hoặc giáo viên chủ bài (chỉ xem). */
	@GetMapping("/api/papers/groups/{groupId}/references")
	@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
	public ApiResponse<List<ReferenceDto>> list(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long groupId) {
		return ApiResponse.ok(service.list(me, groupId));
	}

	@PostMapping("/api/papers/groups/{groupId}/references")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<ReferenceDto> add(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long groupId,
			@Valid @RequestBody ReferenceRequest body) {
		return ApiResponse.ok(service.add(me.id(), groupId, body.csl()), "Đã thêm tài liệu tham khảo");
	}

	@PostMapping("/api/papers/groups/{groupId}/references/batch")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<BatchResult> addBatch(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long groupId,
			@Valid @RequestBody BatchRequest body) {
		return ApiResponse.ok(service.addBatch(me.id(), groupId, body.items()));
	}

	@PutMapping("/api/papers/groups/{groupId}/references/{id}")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<ReferenceDto> update(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long groupId,
			@PathVariable Long id, @Valid @RequestBody ReferenceRequest body) {
		return ApiResponse.ok(service.update(me.id(), groupId, id, body.csl()), "Đã lưu tài liệu tham khảo");
	}

	@DeleteMapping("/api/papers/groups/{groupId}/references/{id}")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<Void> delete(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long groupId,
			@PathVariable Long id) {
		service.delete(me.id(), groupId, id);
		return ApiResponse.message("Đã xoá tài liệu tham khảo");
	}

	/** DOI chứa dấu {@code /} nên đi bằng tham số, không đặt trong đường dẫn. */
	@GetMapping("/api/papers/doi")
	@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
	public ApiResponse<DoiLookup> lookupDoi(@AuthenticationPrincipal AuthPrincipal me, @RequestParam String value) {
		return ApiResponse.ok(service.lookupDoi(me.id(), value));
	}
}
