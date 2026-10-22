package com.ltmtu.ielts.submission;

/** Trạng thái bài nộp của một phần — docs/06-trang-thai.md mục 2. */
public enum SubmissionStatus {
	NOT_STARTED, DRAFT, SUBMITTED, GRADED, RETURNED, REVISION_REQUESTED;

	/** Sinh viên còn soạn / nộp được phần này (chưa nộp, hoặc đang bị yêu cầu sửa). */
	public boolean isOpenForStudent() {
		return this == NOT_STARTED || this == DRAFT || this == REVISION_REQUESTED;
	}

	/** BR-13: phía sinh viên, "đang chấm" hiển thị như "đã nộp" vì điểm chưa công bố. */
	public SubmissionStatus forStudent() {
		return this == GRADED ? SUBMITTED : this;
	}
}
