package com.ltmtu.ielts.common;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body khi lỗi, cùng khung với {@link ApiResponse}: { success: false, data: null, errorCode, errorMessage, ... }.
 * HTTP status vẫn là mã thật (400, 401, 404, 409…) và lặp lại trong trường {@code status}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
		boolean success,
		@JsonInclude(JsonInclude.Include.ALWAYS) Object data,
		String errorCode,
		String errorMessage,
		int status,
		String path,
		Instant timestamp,
		Map<String, String> fieldErrors) {

	public static ErrorResponse of(int status, String code, String message, String path) {
		return of(status, code, message, path, null);
	}

	public static ErrorResponse of(int status, String code, String message, String path, Object data) {
		return new ErrorResponse(false, data, code, message, status, path, Instant.now(), null);
	}

	public static ErrorResponse withFields(int status, String code, String message, String path,
			Map<String, String> fieldErrors) {
		return new ErrorResponse(false, null, code, message, status, path, Instant.now(), fieldErrors);
	}
}
