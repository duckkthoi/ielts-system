package com.ltmtu.ielts.stats;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.stats.GradebookExportService.ExportFile;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.Overview;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.StudentRow;
import com.ltmtu.ielts.user.Role;

import lombok.RequiredArgsConstructor;

/**
 * Thống kê tổng hợp có bộ lọc (H5) — docs/05 mục A.9b. Cùng một bộ API cho admin ({@code /api/admin/analytics…},
 * toàn hệ thống, lọc thêm được theo giáo viên) và giáo viên ({@code /api/teacher/analytics…}, chỉ của mình).
 */
@RestController
@RequiredArgsConstructor
public class AnalyticsController {

	private final AnalyticsService analytics;
	private final AnalyticsExportService exporter;

	@GetMapping({ "/api/admin/analytics", "/api/teacher/analytics" })
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public ApiResponse<Overview> overview(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) Long teacherId, @RequestParam(required = false) Long classId,
			@RequestParam(required = false) Long assignmentId, @RequestParam(required = false) AssignmentKind kind,
			@RequestParam(required = false) AssignmentStatus status, @RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return ApiResponse.ok(analytics.overview(filter(me, teacherId, classId, assignmentId, kind, status, from, to)));
	}

	@GetMapping({ "/api/admin/analytics/students", "/api/teacher/analytics/students" })
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public ApiResponse<List<StudentRow>> students(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) Long teacherId, @RequestParam(required = false) Long classId,
			@RequestParam(required = false) Long assignmentId, @RequestParam(required = false) AssignmentKind kind,
			@RequestParam(required = false) AssignmentStatus status, @RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to, @RequestParam(defaultValue = "true") boolean alertsOnly,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse.page(analytics.students(filter(me, teacherId, classId, assignmentId, kind, status, from, to),
				alertsOnly, page, size));
	}

	@GetMapping({ "/api/admin/analytics/export", "/api/teacher/analytics/export" })
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) Long teacherId, @RequestParam(required = false) Long classId,
			@RequestParam(required = false) Long assignmentId, @RequestParam(required = false) AssignmentKind kind,
			@RequestParam(required = false) AssignmentStatus status, @RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to,
			@RequestParam(defaultValue = "XLSX") AnalyticsExportService.Format format) {
		AnalyticsService.Filter f = filter(me, teacherId, classId, assignmentId, kind, status, from, to);
		ExportFile file = exporter.export(f, analytics.allStudents(f), format);
		MediaType type = format == AnalyticsExportService.Format.CSV ? new MediaType("text", "csv", StandardCharsets.UTF_8)
				: MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		return ResponseEntity.ok()
				.contentType(type)
				.contentLength(file.content().length)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(file.content());
	}

	/** Admin: lọc được theo giáo viên. Giáo viên: luôn là chính mình (bỏ qua {@code teacherId} client gửi). */
	private AnalyticsService.Filter filter(AuthPrincipal me, Long teacherId, Long classId, Long assignmentId,
			AssignmentKind kind, AssignmentStatus status, LocalDate from, LocalDate to) {
		boolean admin = me.role() == Role.ADMIN;
		return analytics.filter(admin ? teacherId : me.id(), classId, assignmentId, kind, status, from, to, admin);
	}
}
