package com.ltmtu.ielts.submission;

/** Sự kiện của module bài nộp — module thông báo báo cho giáo viên (FR-53); Mức 2 dùng thêm cho tiến độ live. */
public final class SubmissionEvents {

	private SubmissionEvents() {
	}

	public record Submitted(Long submissionId, Long assignmentId, Long partId, Long studentId, Long teacherId,
			int versionNo, boolean late, String assignmentTitle, String partTitle, String studentName) {
	}

	/** Lần lưu nháp đầu tiên: phần chuyển từ "Chưa làm" sang "Đang làm" (tiến độ live, FR-61). */
	public record DraftStarted(Long submissionId, Long assignmentId, Long partId, Long studentId) {
	}
}
