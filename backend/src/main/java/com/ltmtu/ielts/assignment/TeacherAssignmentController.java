package com.ltmtu.ielts.assignment;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentDetail;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentSettingsRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentSummary;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.RubricTemplateDto;
import com.ltmtu.ielts.assignment.dto.PublishDtos.PublishResult;
import com.ltmtu.ielts.assignment.dto.PublishDtos.RemindRequest;
import com.ltmtu.ielts.assignment.dto.PublishDtos.RemindResult;
import com.ltmtu.ielts.assignment.dto.PublishDtos.Targets;
import com.ltmtu.ielts.assignment.dto.PublishDtos.TargetsAdded;
import com.ltmtu.ielts.assignment.dto.PublishDtos.TargetsRequest;
import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API bài tập của giáo viên — docs/05-api.md mục A.5. */
@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherAssignmentController {

	private final AssignmentService assignmentService;
	private final PublishService publishService;
	private final BandDescriptorService bandDescriptorService;

	@GetMapping("/rubric-templates")
	public ApiResponse<List<RubricTemplateDto>> templates(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) TaskType taskType) {
		return ApiResponse.ok(assignmentService.templates(me.id(), taskType));
	}

	@GetMapping("/rubric-templates/descriptors")
	public ApiResponse<Map<String, Object>> descriptors(@RequestParam(required = false) TaskType taskType) {
		return ApiResponse.ok(bandDescriptorService.forTaskType(taskType));
	}

	@GetMapping("/assignments")
	public ApiResponse<List<AssignmentSummary>> list(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) AssignmentStatus status, @RequestParam(required = false) String keyword,
			Pageable pageable) {
		return ApiResponse.page(assignmentService.search(me.id(), status, keyword, pageable));
	}

	@PostMapping("/assignments")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AssignmentDetail> create(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody AssignmentRequest body) {
		return ApiResponse.ok(assignmentService.create(me.id(), body), "Đã lưu bài tập nháp");
	}

	@GetMapping("/assignments/{id}")
	public ApiResponse<AssignmentDetail> detail(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(assignmentService.detail(me.id(), id));
	}

	@PutMapping("/assignments/{id}")
	public ApiResponse<AssignmentDetail> replace(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody AssignmentRequest body) {
		return ApiResponse.ok(assignmentService.replaceDraft(me.id(), id, body), "Đã lưu bài tập nháp");
	}

	@PatchMapping("/assignments/{id}/settings")
	public ApiResponse<AssignmentDetail> settings(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody AssignmentSettingsRequest body) {
		return ApiResponse.ok(assignmentService.updateSettings(me.id(), id, body), "Đã cập nhật bài tập");
	}

	@DeleteMapping("/assignments/{id}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		assignmentService.delete(me.id(), id);
		return ApiResponse.message("Đã xoá bài tập");
	}

	// ─── Giao bài và vòng đời (UC-14, UC-15) ─────────────────────────────────

	@PostMapping("/assignments/{id}/publish")
	public ApiResponse<PublishResult> publish(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody TargetsRequest body) {
		return ApiResponse.ok(publishService.publish(me.id(), id, body), "Đã phát hành bài tập");
	}

	@GetMapping("/assignments/{id}/targets")
	public ApiResponse<Targets> targets(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(publishService.targets(me.id(), id));
	}

	@PutMapping("/assignments/{id}/targets")
	public ApiResponse<TargetsAdded> addTargets(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody TargetsRequest body) {
		return ApiResponse.ok(publishService.addMoreTargets(me.id(), id, body));
	}

	@PostMapping("/assignments/{id}/close")
	public ApiResponse<AssignmentDetail> close(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(publishService.close(me.id(), id), "Đã đóng bài tập");
	}

	@PostMapping("/assignments/{id}/reopen")
	public ApiResponse<AssignmentDetail> reopen(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(publishService.reopen(me.id(), id), "Đã mở lại bài tập");
	}

	@PostMapping("/assignments/{id}/remind")
	public ApiResponse<RemindResult> remind(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody RemindRequest body) {
		return ApiResponse.ok(publishService.remind(me.id(), id, body), "Đã gửi nhắc nhở");
	}

	@PostMapping("/assignments/{id}/duplicate")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AssignmentDetail> duplicate(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(publishService.duplicate(me.id(), id), "Đã tạo bản sao (nháp)");
	}
}
