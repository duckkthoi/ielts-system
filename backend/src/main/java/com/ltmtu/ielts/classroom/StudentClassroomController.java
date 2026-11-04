package com.ltmtu.ielts.classroom;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.classroom.dto.ClassroomDtos.StudentClassroomSummary;
import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/** API để sinh viên xem các lớp mình đang tham gia. */
@RestController
@RequestMapping("/api/student/classes")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentClassroomController {

	private final ClassMemberRepository memberRepository;

	@GetMapping
	public ApiResponse<List<StudentClassroomSummary>> list(@AuthenticationPrincipal AuthPrincipal me) {
		return ApiResponse.ok(memberRepository.findClassesOfStudent(me.id()));
	}
}
