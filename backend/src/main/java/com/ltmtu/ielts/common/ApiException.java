package com.ltmtu.ielts.common;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Lỗi nghiệp vụ có mã HTTP và mã lỗi, message hiển thị thẳng cho người dùng (tiếng Việt). */
@Getter
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	/** Dữ liệu kèm lỗi, trả trong trường {@code data} của body lỗi (vd bản nháp trên server khi xung đột). */
	private final transient Object data;

	public ApiException(HttpStatus status, String code, String message) {
		this(status, code, message, null);
	}

	public ApiException(HttpStatus status, String code, String message, Object data) {
		super(message);
		this.status = status;
		this.code = code;
		this.data = data;
	}

	public static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

	public static ApiException unauthorized(String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
	}

	public static ApiException forbidden(String message) {
		return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
	}

	public static ApiException notFound(String message) {
		return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
	}

	public static ApiException conflict(String code, String message) {
		return new ApiException(HttpStatus.CONFLICT, code, message);
	}
}
