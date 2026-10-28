package com.ltmtu.ielts.assignment;

import java.time.Instant;
import java.util.Set;

/**
 * Sự kiện của module bài tập, phát qua {@code ApplicationEventPublisher}.
 * Module thông báo (B7) lắng nghe bằng {@code @TransactionalEventListener(AFTER_COMMIT)} để gửi thông báo / email
 * chỉ khi dữ liệu đã lưu thành công (AI-CODING-RULES mục 11).
 */
public final class AssignmentEvents {

	private AssignmentEvents() {
	}

	/**
	 * Bài vừa được phát hành. {@code opensNow = false} khi {@code open_at} ở tương lai: thông báo sẽ do job gửi đúng
	 * thời điểm mở (SD-05).
	 */
	public record Published(Long assignmentId, String title, Instant dueAt, Set<Long> recipientIds, boolean opensNow) {
	}

	/**
	 * Bài phát hành trước giờ mở vừa tới {@code open_at}: gửi thông báo "Bài tập mới" cho mọi người nhận lúc này
	 * (SD-05 ghi chú). Mỗi bài chỉ phát một lần ({@code open_notified_at}).
	 */
	public record Opened(Long assignmentId, String title, Instant dueAt, Set<Long> recipientIds) {
	}

	/** Bài PAPER tới hạn chốt nhóm (BR-27); {@code recipientIds}: người nhận bài lúc đó — module paper tính ai chưa có nhóm. */
	public record GroupLockReached(Long assignmentId, Long teacherId, String title, Set<Long> recipientIds) {
	}

	/** Giáo viên nhắc thủ công (UC-16). {@code partTitle} null = nhắc cả bài. */
	public record ManualReminder(Long assignmentId, String title, String partTitle, Instant dueAt,
			Set<Long> studentIds) {
	}

	/** Giao thêm người nhận cho bài đã phát hành — chỉ những người MỚI (UC-14 A2). */
	public record RecipientsAdded(Long assignmentId, String title, Instant dueAt, Set<Long> newRecipientIds,
			boolean opensNow) {
	}
}
