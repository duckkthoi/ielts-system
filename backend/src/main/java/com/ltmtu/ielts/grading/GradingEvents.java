package com.ltmtu.ielts.grading;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Sự kiện domain của module chấm bài. Module thông báo nghe (BEFORE_COMMIT) để báo cho sinh viên. */
public final class GradingEvents {

	private GradingEvents() {
	}

	/**
	 * Trả bài (UC-18): sinh viên bắt đầu thấy điểm (BR-13). {@code band} đã làm tròn để hiển thị. {@code recipientIds}:
	 * người nhận kết quả — bài luận là {@code studentId}, bài PAPER là mọi thành viên nhóm (BR-28).
	 */
	/** {@code band}: điểm hiển thị — bài PAPER ({@code paper = true}) là thang 10 (BR-31). */
	public record Returned(Long submissionId, Long assignmentId, Long partId, Long studentId, int versionNo,
			BigDecimal band, String assignmentTitle, String partTitle, List<Long> recipientIds, boolean paper) {
	}

	/** Yêu cầu sửa (UC-18 A1–A3): kết quả đã được trả kèm lời nhắn và hạn sửa. */
	public record RevisionRequested(Long submissionId, Long assignmentId, Long partId, Long studentId, int versionNo,
			BigDecimal band, String note, Instant revisionDueAt, String assignmentTitle, String partTitle,
			List<Long> recipientIds) {
	}
}
