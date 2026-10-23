package com.ltmtu.ielts.stats;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.stats.dto.StudentStatsDtos.StudentStats;

import lombok.RequiredArgsConstructor;

/** Kết quả học tập của sinh viên (UC-28) — docs/05-api.md mục A.8. */
@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentStatsController {

	private final StudentStatsService studentStatsService;

	@GetMapping("/results")
	public ApiResponse<StudentStats> results(@AuthenticationPrincipal AuthPrincipal me) {
		return ApiResponse.ok(studentStatsService.mine(me.id()));
	}
}
