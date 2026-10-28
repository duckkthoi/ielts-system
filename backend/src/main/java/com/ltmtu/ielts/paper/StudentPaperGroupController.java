package com.ltmtu.ielts.paper;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.paper.dto.PaperGroupDtos.CreateGroupRequest;
import com.ltmtu.ielts.paper.dto.PaperGroupDtos.StudentGroups;
import com.ltmtu.ielts.security.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Sinh viên tự lập / vào / rời nhóm bài PAPER — docs/05-api.md mục A.10. */
@RestController
@RequestMapping("/api/student/assignments/{id}/groups")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentPaperGroupController {

	private final StudentPaperGroupService service;

	@GetMapping
	public ApiResponse<StudentGroups> groups(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(service.groups(me.id(), id));
	}

	@PostMapping
	public ApiResponse<StudentGroups> create(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody CreateGroupRequest body) {
		return ApiResponse.ok(service.create(me.id(), id, body.name()), "Đã lập nhóm");
	}

	@PostMapping("/{groupId}/join")
	public ApiResponse<StudentGroups> join(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@PathVariable Long groupId) {
		return ApiResponse.ok(service.join(me.id(), id, groupId), "Đã vào nhóm");
	}

	@DeleteMapping("/mine")
	public ApiResponse<StudentGroups> leave(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(service.leave(me.id(), id), "Đã rời nhóm");
	}
}
