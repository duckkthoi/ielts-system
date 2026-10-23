package com.ltmtu.ielts.submission.dto;

import java.time.Instant;

import com.ltmtu.ielts.submission.SubmissionStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO làm bài / nộp bài phía sinh viên — docs/05-api.md mục A.8. */
public final class SubmissionDtos {

	/** Giới hạn độ dài bài làm (~ 5000 từ) để chặn request bất thường. */
	public static final int MAX_CONTENT = 30_000;

	private SubmissionDtos() {
	}

	public enum DraftSource {
		/** Bản nháp đang lưu. */
		DRAFT,
		/** Đang "Cần sửa" mà chưa có nháp → nạp nội dung version mới nhất để sửa tiếp (UC-26 bước 2). */
		LAST_VERSION,
		/** Chưa có gì. */
		EMPTY
	}

	/** {@code draftVersion}: gửi lại ở lần lưu nháp sau để phát hiện tab / máy khác đã lưu (SD-06). */
	public record DraftResponse(String content, DraftSource source, Instant savedAt, int wordCount,
			SubmissionStatus status, int currentVersion, int draftVersion) {
	}

	/** {@code draftVersion} bỏ trống = không kiểm tra xung đột (ghi đè như Mức 1). */
	public record SaveDraftRequest(
			@NotNull(message = "Thiếu nội dung") @Size(max = MAX_CONTENT, message = "Bài quá dài") String content,
			@Min(value = 0, message = "draftVersion không hợp lệ") Integer draftVersion) {
	}

	public record SaveDraftResponse(Instant savedAt, int wordCount, int draftVersion) {
	}

	/** {@code data} của lỗi 409 DRAFT_CONFLICT: bản đang lưu trên server, để client cho người dùng chọn bản. */
	public record DraftConflict(String serverContent, int draftVersion, Instant savedAt) {
	}

	public record SubmitRequest(
			@NotBlank(message = "Nội dung không được để trống") @Size(max = MAX_CONTENT, message = "Bài quá dài") String content,
			@NotNull(message = "Thiếu expectedVersion") @Min(0) Integer expectedVersion) {
	}

	public record SubmitResponse(int versionNo, Instant submittedAt, boolean late, int wordCount) {
	}
}
