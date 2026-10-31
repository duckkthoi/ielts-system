package com.ltmtu.ielts.user;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.ApiResponse;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.dto.AdminUserDtos.CreateTeacherRequest;
import com.ltmtu.ielts.user.dto.AdminUserDtos.CreatedTeacher;
import com.ltmtu.ielts.user.dto.AdminUserDtos.TeacherImportReport;
import com.ltmtu.ielts.user.dto.AdminUserDtos.TemporaryPassword;
import com.ltmtu.ielts.user.dto.AdminUserDtos.UpdateTeacherRequest;
import com.ltmtu.ielts.user.dto.AdminUserDtos.UserDetail;
import com.ltmtu.ielts.user.dto.AdminUserDtos.UserSummary;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API quản trị người dùng — docs/05-api.md mục A.3. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

	private final AdminUserService adminUserService;
	private final TeacherImportService teacherImportService;

	@GetMapping("/teachers")
	public ApiResponse<List<UserSummary>> teachers(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Boolean active, Pageable pageable) {
		return ApiResponse.page(adminUserService.search(keyword, Role.TEACHER, active, pageable));
	}

	@PostMapping("/teachers")
	public ApiResponse<CreatedTeacher> createTeacher(@AuthenticationPrincipal AuthPrincipal me,
			@Valid @RequestBody CreateTeacherRequest body) {
		return ApiResponse.ok(adminUserService.createTeacher(me, body), "Đã tạo tài khoản giáo viên");
	}

	/** Import nhiều giáo viên từ file .xlsx / .csv (UC-05b). Mật khẩu tạm chỉ trả trong response này. */
	@PostMapping(path = "/teachers/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<TeacherImportReport> importTeachers(@AuthenticationPrincipal AuthPrincipal me,
			@RequestParam("file") MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw ApiException.badRequest("BAD_FILE", "File rỗng");
		}
		try (InputStream in = file.getInputStream()) {
			TeacherImportReport report = teacherImportService.importFile(me, file.getOriginalFilename(), in);
			return ApiResponse.ok(report, "Đã import " + report.created() + " giáo viên");
		}
	}

	@GetMapping("/teachers/import-template")
	public ResponseEntity<byte[]> teacherTemplate(@RequestParam(defaultValue = "xlsx") String format) {
		boolean csv = "csv".equalsIgnoreCase(format);
		byte[] content = csv ? TeacherImportService.templateCsv() : TeacherImportService.templateXlsx();
		MediaType type = csv ? new MediaType("text", "csv", StandardCharsets.UTF_8)
				: MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		String filename = "mau-danh-sach-giao-vien." + (csv ? "csv" : "xlsx");
		return ResponseEntity.ok()
				.contentType(type)
				.contentLength(content.length)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
				.body(content);
	}

	@PutMapping("/teachers/{id}")
	public ApiResponse<UserSummary> updateTeacher(@PathVariable Long id, @Valid @RequestBody UpdateTeacherRequest body) {
		return ApiResponse.ok(adminUserService.updateTeacher(id, body));
	}

	@GetMapping("/users")
	public ApiResponse<List<UserSummary>> users(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Role role, @RequestParam(required = false) Boolean active,
			Pageable pageable) {
		return ApiResponse.page(adminUserService.search(keyword, role, active, pageable));
	}

	@GetMapping("/users/{id}")
	public ApiResponse<UserDetail> user(@PathVariable Long id) {
		return ApiResponse.ok(adminUserService.detail(id));
	}

	@PostMapping("/users/{id}/lock")
	public ApiResponse<UserSummary> lock(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(adminUserService.lock(me, id), "Đã khoá tài khoản");
	}

	@PostMapping("/users/{id}/unlock")
	public ApiResponse<UserSummary> unlock(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(adminUserService.unlock(me, id), "Đã mở khoá tài khoản");
	}

	@PostMapping("/users/{id}/reset-password")
	public ApiResponse<TemporaryPassword> resetPassword(@AuthenticationPrincipal AuthPrincipal me,
			@PathVariable Long id) {
		return ApiResponse.ok(adminUserService.resetPassword(me, id), "Đã đặt lại mật khẩu");
	}
}
