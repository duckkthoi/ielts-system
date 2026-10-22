package com.ltmtu.ielts.stats;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.stats.SystemStatsService.SystemStats;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminStatsController {

	private final SystemStatsService systemStatsService;

	@GetMapping("/stats")
	public ApiResponse<SystemStats> stats() {
		return ApiResponse.ok(systemStatsService.stats());
	}
}
