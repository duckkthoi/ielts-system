package com.ltmtu.ielts.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ltmtu.ielts.assignment.AssignmentEvents;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.grading.GradingEvents;
import com.ltmtu.ielts.notification.NotificationService.NewNotification;
import com.ltmtu.ielts.submission.SubmissionEvents;

/**
 * Biến sự kiện nghiệp vụ thành thông báo (UC-04, UC-16; FR-50, FR-51, FR-53).
 * <p>
 * Nghe ở pha {@code BEFORE_COMMIT}: chạy trong chính transaction của nghiệp vụ, ngay trước khi commit. Thông báo vì
 * vậy được lưu <b>cùng lúc</b> với việc phát hành / nộp / trả bài — commit thì có cả hai, rollback thì không có gì.
 * Việc đẩy WebSocket và gửi email do {@link NotificationService} dời tới sau khi commit.
 */
@Component
public class NotificationEventListener {

	private final NotificationService notifications;
	private final ReminderLogRepository reminderLogRepository;
	private final DateTimeFormatter timeFormat;

	public NotificationEventListener(NotificationService notifications, ReminderLogRepository reminderLogRepository,
			AppProperties props) {
		this.notifications = notifications;
		this.reminderLogRepository = reminderLogRepository;
		this.timeFormat = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(props.zone());
	}

	/** SD-05: bài mới. Bài mở trong tương lai ({@code opensNow = false}) thì job gửi đúng lúc mở (B7b). */
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onPublished(AssignmentEvents.Published e) {
		if (e.opensNow()) {
			notifications.notify(e.recipientIds(), newAssignment(e.assignmentId(), e.title(), e.dueAt()));
		}
	}

	/** Bài phát hành trước giờ mở vừa tới giờ mở (job {@code AssignmentOpeningService}). */
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onOpened(AssignmentEvents.Opened e) {
		notifications.notify(e.recipientIds(), newAssignment(e.assignmentId(), e.title(), e.dueAt()));
	}

	/** UC-16: giáo viên nhắc thủ công. */
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onManualReminder(AssignmentEvents.ManualReminder e) {
		String what = e.title() + (e.partTitle() == null ? "" : " – " + e.partTitle());
		notifications.notify(e.studentIds(), new NewNotification(NotificationType.MANUAL_REMINDER,
				"Giáo viên nhắc bạn hoàn thành: " + what,
				e.dueAt() == null ? null : "Hạn nộp: " + timeFormat.format(e.dueAt()) + ".", studentLink(e.assignmentId())));
	}

	/** UC-14 A2: giao thêm — chỉ người mới nhận thông báo. */
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onRecipientsAdded(AssignmentEvents.RecipientsAdded e) {
		if (e.opensNow()) {
			notifications.notify(e.newRecipientIds(), newAssignment(e.assignmentId(), e.title(), e.dueAt()));
		}
	}

	/** FR-53: giáo viên biết ngay khi sinh viên nộp; bấm vào mở thẳng màn chấm. Không gửi email (tránh hộp thư đầy). */
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onSubmitted(SubmissionEvents.Submitted e) {
		String what = e.versionNo() > 1 ? "nộp bản sửa (lần " + e.versionNo() + ")" : "nộp bài";
		notifications.notify(Set.of(e.teacherId()), new NewNotification(NotificationType.SUBMISSION_RECEIVED,
				e.studentName() + " đã " + what + ": " + e.assignmentTitle() + " – " + e.partTitle(),
				e.late() ? "Nộp muộn so với hạn." : null, "/giao-vien/cham-bai/" + e.submissionId()));
	}

	/** UC-18: đã có kết quả. */
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onReturned(GradingEvents.Returned e) {
		notifications.notify(Set.of(e.studentId()), new NewNotification(NotificationType.GRADE_RETURNED,
				"Đã có kết quả: " + e.assignmentTitle() + " – " + e.partTitle(), "Band: " + band(e.band()),
				studentLink(e.assignmentId())));
	}

	/**
	 * UC-18 A3: cần sửa bài, kèm hạn sửa và lời nhắn. Xoá mốc nhắc hạn sửa của vòng sửa trước, để vòng này vẫn được
	 * nhắc (BR-14 tính theo từng hạn sửa).
	 */
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onRevisionRequested(GradingEvents.RevisionRequested e) {
		reminderLogRepository.resetRevision(e.partId(), e.studentId());
		String body = "Hạn sửa: " + timeFormat.format(e.revisionDueAt()) + "."
				+ (e.note() == null ? "" : " Lời nhắn: " + e.note());
		notifications.notify(Set.of(e.studentId()), new NewNotification(NotificationType.REVISION_REQUESTED,
				"Cần sửa bài: " + e.assignmentTitle() + " – " + e.partTitle(), body, studentLink(e.assignmentId())));
	}

	private NewNotification newAssignment(Long assignmentId, String title, Instant dueAt) {
		return new NewNotification(NotificationType.ASSIGNMENT_PUBLISHED, "Bài tập mới: " + title,
				dueAt == null ? null : "Hạn nộp: " + timeFormat.format(dueAt) + ".", studentLink(assignmentId));
	}

	private static String studentLink(Long assignmentId) {
		return "/sinh-vien/bai-tap/" + assignmentId;
	}

	private static String band(BigDecimal band) {
		return band == null ? "—" : band.toPlainString();
	}
}
