package com.ltmtu.ielts.paper.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public final class PaperLiveCommentDtos {

	private PaperLiveCommentDtos() {
	}

	public record CreateRequest(@Min(value = 0, message = "Vị trí bắt đầu không hợp lệ") int startOffset,
			@Min(value = 1, message = "Vị trí kết thúc không hợp lệ") int endOffset,
			@NotBlank(message = "Đoạn được chọn không được trống") @Size(max = 10000) String quote,
			@NotBlank(message = "Nhận xét không được trống") @Size(max = 5000) String comment) {
	}

	public record UpdateRequest(
			@NotBlank(message = "Nhận xét không được trống") @Size(max = 5000) String comment) {
	}

	public record CommentDto(Long id, Long documentId, Long authorId, String authorName, int startOffset, int endOffset,
			String quote, String comment, Instant createdAt, Instant updatedAt) {
	}

	public record CommentPush(String type, CommentDto comment, Long commentId) {
	}

	public static List<CommentDto> empty() {
		return List.of();
	}
}
