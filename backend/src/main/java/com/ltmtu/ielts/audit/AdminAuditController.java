package com.ltmtu.ielts.audit;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.audit.AuditService.AuditLogDto;
import com.ltmtu.ielts.common.ApiResponse;

import lombok.RequiredArgsConstructor;

/** Nhật ký hệ thống cho admin (UC-07) — docs/05-api.md mục A.3. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAuditController {

	private final AuditService auditService;

	@GetMapping("/audit-logs")
	public ApiResponse<List<AuditLogDto>> search(@RequestParam(required = false) Long actorId,
			@RequestParam(required = false) AuditAction action, @RequestParam(required = false) String ip,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			Pageable pageable) {
		return ApiResponse.page(auditService.search(actorId, action, ip, from, to, pageable));
	}
}
