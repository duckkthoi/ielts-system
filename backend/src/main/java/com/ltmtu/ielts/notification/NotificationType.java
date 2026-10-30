package com.ltmtu.ielts.notification;

/** Loại thông báo — docs/04-erd.md mục 4.6. {@code email}: có gửi kèm email không (FR-51). */
public enum NotificationType {
	ASSIGNMENT_PUBLISHED(true),
	SUBMISSION_RECEIVED(false),
	GRADE_RETURNED(true),
	REVISION_REQUESTED(true),
	DEADLINE_REMINDER(true),
	MANUAL_REMINDER(true),
	/** Chế độ thi: sinh viên chuyển tab / dán văn bản (gửi giáo viên). */
	EXAM_ALERT(false),
	/** Chế độ thi: hết giờ mà chưa nộp (gửi giáo viên và sinh viên). */
	EXAM_OVERTIME(false),
	/** Bài paper: tới hạn chốt nhóm mà còn sinh viên chưa có nhóm (gửi giáo viên). */
	PAPER_GROUP_LOCK(false);

	private final boolean email;

	NotificationType(boolean email) {
		this.email = email;
	}

	public boolean sendsEmail() {
		return email;
	}
}
