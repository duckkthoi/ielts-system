package com.ltmtu.ielts.classroom;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ltmtu.ielts.classroom.dto.ClassroomDtos.AddMembersRequest;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.AddMembersResult;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.ClassroomRequest;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.ClassroomSummary;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.MemberDto;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.StudentOption;
import com.ltmtu.ielts.classroom.dto.ImportDtos.ImportReport;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API lớp học của giáo viên — docs/05-api.md mục A.4. */
@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherClassroomController {

	private final ClassroomService classroomService;
	private final StudentImportService importService;

	@GetMapping("/classes")
	public ApiResponse<List<ClassroomSummary>> list(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam(required = false) String keyword, Pageable pageable) {
		return ApiResponse.page(classroomService.search(me.id(), keyword, pageable));
	}

	@PostMapping("/classes")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ClassroomSummary> create(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody ClassroomRequest body) {
		return ApiResponse.ok(classroomService.create(me.id(), body), "Đã tạo lớp");
	}

	@GetMapping("/classes/{id}")
	public ApiResponse<ClassroomSummary> detail(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(classroomService.detail(me.id(), id));
	}

	@PutMapping("/classes/{id}")
	public ApiResponse<ClassroomSummary> update(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody ClassroomRequest body) {
		return ApiResponse.ok(classroomService.update(me.id(), id, body));
	}

	@DeleteMapping("/classes/{id}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		classroomService.delete(me.id(), id);
		return ApiResponse.message("Đã xoá lớp");
	}

	@GetMapping("/classes/{id}/members")
	public ApiResponse<List<MemberDto>> members(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@RequestParam(required = false) String keyword, Pageable pageable) {
		return ApiResponse.page(classroomService.members(me.id(), id, keyword, pageable));
	}

	@PostMapping("/classes/{id}/members")
	public ApiResponse<AddMembersResult> addMembers(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@Valid @RequestBody AddMembersRequest body) {
		return ApiResponse.ok(classroomService.addMembers(me.id(), id, body.studentCodes()));
	}

	@DeleteMapping("/classes/{id}/members/{studentId}")
	public ApiResponse<Void> removeMember(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@PathVariable Long studentId) {
		classroomService.removeMember(me.id(), id, studentId);
		return ApiResponse.message("Đã xoá sinh viên khỏi lớp");
	}

	@GetMapping("/students/search")
	public ApiResponse<List<StudentOption>> searchStudents(@RequestParam(required = false) String keyword) {
		return ApiResponse.ok(classroomService.searchStudents(keyword));
	}

	// ─── Import sinh viên (UC-11, SD-03) ──────────────────────────────────────

	@PostMapping(path = "/classes/{id}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<ImportReport> importStudents(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id,
			@RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "false") boolean sendEmail)
			throws IOException {
		if (file.isEmpty()) {
			throw ApiException.badRequest("BAD_FILE", "File rỗng");
		}
		try (InputStream in = file.getInputStream()) {
			return ApiResponse.ok(importService.importFile(me.id(), id, file.getOriginalFilename(), in, sendEmail));
		}
	}

	/** File tài khoản + mật khẩu tạm vừa tạo — chỉ tải được một lần, hết hạn sau 15 phút. */
	@GetMapping("/imports/{token}/credentials")
	public ResponseEntity<byte[]> credentials(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String token) {
		StudentImportService.CredentialsFile file = importService.takeCredentials(me.id(), token);
		return download(file.filename(), XLSX, file.content())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(file.content());
	}

	@GetMapping("/import-template")
	public ResponseEntity<byte[]> template(@RequestParam(defaultValue = "xlsx") String format) {
		if ("csv".equalsIgnoreCase(format)) {
			byte[] csv = ImportWorkbooks.templateCsv();
			return download("mau-danh-sach-sinh-vien.csv", new MediaType("text", "csv", StandardCharsets.UTF_8), csv)
					.body(csv);
		}
		byte[] xlsx = ImportWorkbooks.templateXlsx();
		return download("mau-danh-sach-sinh-vien.xlsx", XLSX, xlsx).body(xlsx);
	}

	private static final MediaType XLSX = MediaType
			.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private static ResponseEntity.BodyBuilder download(String filename, MediaType type, byte[] content) {
		return ResponseEntity.ok()
				.contentType(type)
				.contentLength(content.length)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString());
	}
}
