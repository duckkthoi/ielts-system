package com.ltmtu.ielts.common;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Dạng response chung cho mọi API: { success, data, message, totalRecord }.
 * Frontend (axiosClient) dựa vào {@code success} để quyết định hiển thị lỗi.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String message, Long totalRecord) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null, null);
	}

	public static <T> ApiResponse<T> ok(T data, String message) {
		return new ApiResponse<>(true, data, message, null);
	}

	public static ApiResponse<Void> message(String message) {
		return new ApiResponse<>(true, null, message, null);
	}

	/** Trả danh sách của một trang kèm tổng số bản ghi. */
	public static <T> ApiResponse<java.util.List<T>> page(Page<T> page) {
		return new ApiResponse<>(true, page.getContent(), null, page.getTotalElements());
	}
}
