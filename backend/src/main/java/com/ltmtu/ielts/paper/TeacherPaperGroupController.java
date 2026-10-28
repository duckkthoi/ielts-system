package com.ltmtu.ielts.paper;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.paper.dto.PaperGroupDtos.AutoGroupRequest;
import com.ltmtu.ielts.paper.dto.PaperGroupDtos.GroupRequest;
import com.ltmtu.ielts.paper.dto.PaperGroupDtos.TeacherGroups;
import com.ltmtu.ielts.security.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Giáo viên chia nhóm bài PAPER — docs/05-api.md mục A.10. Mọi thao tác trả lại toàn bộ màn chia nhóm. */
@RestController
@RequestMapping("/api/teacher/assignments/{id}/groups")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherPaperGroupController {

	private final TeacherPaperGroupService service;

	@GetMapping
	public ApiResponse<TeacherGroups> groups(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(service.groups(me.id(), id));
	}

	@PostMapping
	public ApiResponse<TeacherGroups> create(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody GroupRequest body) {
		return ApiResponse.ok(service.create(me.id(), id, body), "Đã tạo nhóm");
	}

	@PutMapping("/{groupId}")
	public ApiResponse<TeacherGroups> update(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@PathVariable Long groupId, @Valid @RequestBody GroupRequest body) {
		return ApiResponse.ok(service.update(me.id(), id, groupId, body), "Đã lưu nhóm");
	}

	@DeleteMapping("/{groupId}")
	public ApiResponse<TeacherGroups> delete(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@PathVariable Long groupId) {
		return ApiResponse.ok(service.delete(me.id(), id, groupId), "Đã xoá nhóm");
	}

	@PostMapping("/auto")
	public ApiResponse<TeacherGroups> auto(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@RequestBody(required = false) AutoGroupRequest body) {
		return ApiResponse.ok(service.autoSplit(me.id(), id, body == null ? null : body.classId()),
				"Đã chia nhóm ngẫu nhiên");
	}
}
