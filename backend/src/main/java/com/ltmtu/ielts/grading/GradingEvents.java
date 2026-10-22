package com.ltmtu.ielts.grading;

import java.math.BigDecimal;
import java.time.Instant;

/** Sự kiện domain của module chấm bài. Module thông báo nghe (BEFORE_COMMIT) để báo cho sinh viên. */
public final class GradingEvents {

	private GradingEvents() {
	}

	/** Trả bài (UC-18): sinh viên bắt đầu thấy điểm (BR-13). {@code band} đã làm tròn để hiển thị. */
	public record Returned(Long submissionId, Long assignmentId, Long partId, Long studentId, int versionNo,
			BigDecimal band, String assignmentTitle, String partTitle) {
	}

	/** Yêu cầu sửa (UC-18 A1–A3): kết quả đã được trả kèm lời nhắn và hạn sửa. */
	public record RevisionRequested(Long submissionId, Long assignmentId, Long partId, Long studentId, int versionNo,
			BigDecimal band, String note, Instant revisionDueAt, String assignmentTitle, String partTitle) {
	}
}
