package com.ltmtu.ielts.paper.dto;

import java.time.Instant;

import com.ltmtu.ielts.paper.PaperLiveComment.Kind;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Nhận xét trực tiếp trên Paper đang viết — docs/05 mục A.10 (H4). */
public final class PaperLiveCommentDtos {

	private PaperLiveCommentDtos() {
	}

	/** {@code kind} null → COMMENT. */
	public record CreateRequest(@Min(value = 0, message = "Vị trí bắt đầu không hợp lệ") int startOffset,
			@Min(value = 1, message = "Vị trí kết thúc không hợp lệ") int endOffset,
			@NotBlank(message = "Đoạn được chọn không được trống")
			@Size(max = 2000, message = "Đoạn được chọn tối đa 2000 ký tự") String quote,
			@NotBlank(message = "Nhận xét không được trống")
			@Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự") String comment,
			Kind kind,
			@Size(max = 2000, message = "Gợi ý sửa tối đa 2000 ký tự") String suggestion,
			@Size(max = 100) @Pattern(regexp = "^[a-z0-9-]*$", message = "Mã mục không hợp lệ") String sectionKey) {
	}

	/** Sửa nội dung / loại / gợi ý; đoạn được chọn (quote, offset) không đổi. */
	public record UpdateRequest(
			@NotBlank(message = "Nhận xét không được trống")
			@Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự") String comment,
			Kind kind,
			@Size(max = 2000, message = "Gợi ý sửa tối đa 2000 ký tự") String suggestion) {
	}

	public record ResolveRequest(boolean resolved) {
	}

	public record CommentDto(Long id, Long documentId, Long authorId, String authorName, int startOffset, int endOffset,
			String quote, String comment, Kind kind, String suggestion, String sectionKey, Instant resolvedAt,
			String resolvedByName, Instant createdAt, Instant updatedAt) {
	}

	public record CommentPush(String type, CommentDto comment, Long commentId) {
	}
}
