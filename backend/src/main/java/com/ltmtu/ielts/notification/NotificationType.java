package com.ltmtu.ielts.notification;

/** Loại thông báo — docs/04-erd.md mục 4.6. {@code email}: có gửi kèm email không (FR-51). */
public enum NotificationType {
	ASSIGNMENT_PUBLISHED(true),
	SUBMISSION_RECEIVED(false),
	GRADE_RETURNED(true),
	REVISION_REQUESTED(true),
	DEADLINE_REMINDER(true),
	MANUAL_REMINDER(true);

	private final boolean email;

	NotificationType(boolean email) {
		this.email = email;
	}

	public boolean sendsEmail() {
		return email;
	}
}
