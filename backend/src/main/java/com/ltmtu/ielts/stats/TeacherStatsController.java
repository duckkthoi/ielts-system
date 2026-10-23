package com.ltmtu.ielts.stats;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.stats.GradebookExportService.ExportFile;
import com.ltmtu.ielts.stats.dto.AssignmentStatsDtos.AssignmentStats;
import com.ltmtu.ielts.stats.dto.ClassStatsDtos.ClassStats;
import com.ltmtu.ielts.stats.dto.ProgressDtos.ProgressMatrix;
import com.ltmtu.ielts.stats.dto.StudentStatsDtos.StudentStats;
import com.ltmtu.ielts.submission.SubmissionStatus;

import lombok.RequiredArgsConstructor;

/** Tiến độ và thống kê phía giáo viên — docs/05-api.md mục A.6. */
@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherStatsController {

	private final ProgressService progressService;
	private final AssignmentStatsService assignmentStatsService;
	private final StudentStatsService studentStatsService;
	private final ClassStatsService classStatsService;
	private final GradebookExportService exportService;

	@GetMapping("/assignments/{id}/progress")
	public ApiResponse<ProgressMatrix> progress(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@RequestParam(required = false) Long classId, @RequestParam(required = false) SubmissionStatus status) {
		return ApiResponse.ok(progressService.progress(me.id(), id, classId, status));
	}

	@GetMapping("/assignments/{id}/stats")
	public ApiResponse<AssignmentStats> stats(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@RequestParam(required = false) Long classId) {
		return ApiResponse.ok(assignmentStatsService.stats(me.id(), id, classId));
	}

	@GetMapping("/stats/students/{id}")
	public ApiResponse<StudentStats> student(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(studentStatsService.ofTeacher(me.id(), id));
	}

	@GetMapping("/stats/classes/{id}")
	public ApiResponse<ClassStats> classroom(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(classStatsService.stats(me.id(), id));
	}

	/** Bảng điểm {@code .xlsx} (FR-66). */
	@GetMapping("/assignments/{id}/export")
	public ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@RequestParam(required = false) Long classId) {
		ExportFile file = exportService.export(me.id(), id, classId);
		return ResponseEntity.ok()
				.contentType(XLSX)
				.contentLength(file.content().length)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(file.content());
	}

	private static final MediaType XLSX = MediaType
			.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
}
