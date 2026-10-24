package com.ltmtu.ielts.common;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

/** Upload ảnh đề bài (docs/05-api.md mục A.5). */
@RestController
@RequiredArgsConstructor
public class FileController {

	private final FileStorageService storage;

	@PostMapping(path = "/api/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
	public ApiResponse<UploadedFile> upload(@RequestParam("file") MultipartFile file) {
		return ApiResponse.ok(new UploadedFile(storage.storeImage(file)));
	}

	public record UploadedFile(String url) {
	}
}
