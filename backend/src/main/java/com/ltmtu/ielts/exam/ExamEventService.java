package com.ltmtu.ielts.exam;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.exam.ExamEnums.ExamEventType;
import com.ltmtu.ielts.exam.dto.ExamDtos.ExamPush;
import com.ltmtu.ielts.notification.NotificationService;
import com.ltmtu.ielts.notification.NotificationService.NewNotification;
import com.ltmtu.ielts.notification.NotificationType;
import com.ltmtu.ielts.realtime.PresenceEvents;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.submission.SubmissionEvents;

import lombok.extern.slf4j.Slf4j;

/**
 * Ghi sự kiện của phiên thi (UC-27, UC-21): sự kiện client báo (rời / quay lại tab, dán), sự kiện server tự ghi (bắt
 * đầu, nộp, mất / có lại kết nối, hết giờ). Mỗi sự kiện được lưu rồi <b>sau commit</b> đẩy tới
 * {@code /topic/assignments/{id}/exam} của giáo viên chủ bài ({@link ExamPushListener}); rời tab / dán còn tạo thông
 * báo cho giáo viên (BR-25), hết giờ báo cả giáo viên lẫn sinh viên (BR-24).
 */
@Slf4j
@Service
public class ExamEventService {

	private final ExamSessionRepository sessionRepository;
	private final ExamEventRepository eventRepository;
	private final ExamThrottle throttle;
	private final NotificationService notifications;
	private final ApplicationEventPublisher events;
	private final Clock clock;
	private final DateTimeFormatter timeFormat;

	public ExamEventService(ExamSessionRepository sessionRepository, ExamEventRepository eventRepository,
			ExamThrottle throttle, NotificationService notifications, ApplicationEventPublisher events, Clock clock,
			AppProperties props) {
		this.sessionRepository = sessionRepository;
		this.eventRepository = eventRepository;
		this.throttle = throttle;
		this.notifications = notifications;
		this.events = events;
		this.clock = clock;
		this.timeFormat = DateTimeFormatter.ofPattern("HH:mm dd/MM").withZone(props.zone());
	}

	/**
	 * Sự kiện sinh viên gửi qua STOMP. Phiên của người khác, loại sự kiện client không được gửi, hoặc gửi quá dày →
	 * bỏ qua (không lỗi, không lộ thông tin), giống {@code /app/presence/viewing}. Phiên đã nộp chỉ nhận sự kiện gửi
	 * bù xảy ra <b>trước</b> lúc nộp ({@code clientAt}: giờ xảy ra client báo khi gửi bù lúc có mạng lại).
	 */
	@Transactional
	public void clientEvent(AuthPrincipal student, Long sessionId, ExamEventType type, Integer pasteChars,
			Instant clientAt) {
		if (sessionId == null || type == null || !type.fromClient()) {
			return;
		}
		ExamSession s = sessionRepository.findFull(sessionId).orElse(null);
		if (s == null || !s.getStudent().getId().equals(student.id())) {
			return;
		}
		Instant now = clock.instant();
		ExamRules.Reported at = ExamRules.reportedAt(clientAt, s.getStartedAt(), now);
		if (s.getFinishedAt() != null && (!at.late() || at.at().isAfter(s.getFinishedAt()))) {
			return;
		}
		if (!throttle.acceptEvent(sessionId)) {
			log.debug("Phiên thi {}: bỏ sự kiện {} vì gửi quá dày", sessionId, type);
			return;
		}
		Integer chars = type == ExamEventType.PASTE && pasteChars != null ? Math.max(0, pasteChars) : null;
		record(s, type, chars, at.at(), at.late(), now);
		if (type.alertsTeacher() && throttle.acceptAlert(sessionId)) {
			alertTeacher(s, type, chars, at.late());
		}
	}

	/** Mất kết nối cuối cùng / có lại kết nối đầu tiên: ghi vào mọi phiên thi chưa nộp của sinh viên. */
	@EventListener
	@Transactional
	public void onOnlineChanged(PresenceEvents.OnlineChanged e) {
		List<ExamSession> sessions = sessionRepository.findUnfinished(e.studentId());
		Instant now = clock.instant();
		for (ExamSession s : sessions) {
			record(s, e.online() ? ExamEventType.RECONNECT : ExamEventType.DISCONNECT, null, now, false, now);
		}
	}

	/**
	 * Lần nộp đầu tiên của phần ở chế độ thi kết thúc phiên. Chạy {@code BEFORE_COMMIT} — trong transaction nộp bài,
	 * nên nộp bị rollback thì phiên không bị đánh dấu đã nộp.
	 */
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onSubmitted(SubmissionEvents.Submitted e) {
		ExamSession s = sessionRepository.findFull(e.partId(), e.studentId()).orElse(null);
		if (s == null || s.getFinishedAt() != null) {
			return;
		}
		Instant now = clock.instant();
		s.setFinishedAt(now);
		record(s, ExamEventType.SUBMITTED, null, now, false, now);
	}

	/** Job (UC-31): một phiên vừa được nhận việc báo quá giờ — ghi sự kiện và báo giáo viên + sinh viên. */
	public void overtime(ExamSession s, Instant now) {
		record(s, ExamEventType.OVERTIME, null, now, false, now);
		AssignmentPart p = s.getPart();
		Assignment a = p.getAssignment();
		String what = a.getTitle() + " – " + p.getTitle();
		notifications.notify(List.of(a.getTeacher().getId()), new NewNotification(NotificationType.EXAM_OVERTIME,
				"Quá thời gian thi: " + s.getStudent().getFullName(),
				what + ". Hết giờ lúc " + timeFormat.format(s.getEndsAt()) + " mà chưa nộp.", monitorLink(a)));
		notifications.notify(List.of(s.getStudent().getId()), new NewNotification(NotificationType.EXAM_OVERTIME,
				"Đã hết thời gian làm bài: " + what,
				"Bạn vẫn nộp được, nhưng bài sẽ được đánh dấu quá thời gian.", "/sinh-vien/lam-bai/" + a.getId()));
		log.info("Phiên thi {} (sinh viên {}) quá giờ", s.getId(), s.getStudent().getId());
	}

	/**
	 * Lưu một sự kiện xảy ra lúc {@code at}; sau commit đẩy lên màn giám sát kèm trạng thái phiên <b>lúc này</b>
	 * ({@code now} — sự kiện gửi bù không làm bảng lùi trạng thái). {@code s} phải nạp sẵn phần, bài và sinh viên.
	 */
	public void record(ExamSession s, ExamEventType type, Integer pasteChars, Instant at, boolean reportedLate,
			Instant now) {
		eventRepository.save(ExamEvent.of(s, type, pasteChars, at, reportedLate));
		events.publishEvent(new ExamEvents.Recorded(s.getPart().getAssignment().getId(),
				new ExamPush(s.getId(), s.getPart().getId(), s.getStudent().getId(), s.getStudent().getFullName(), type,
						pasteChars, ExamRules.status(s.getEndsAt(), s.getFinishedAt(), now), at, reportedLate)));
	}

	private void alertTeacher(ExamSession s, ExamEventType type, Integer chars, boolean reportedLate) {
		Assignment a = s.getPart().getAssignment();
		String name = s.getStudent().getFullName();
		String title = switch (type) {
		case PASTE -> name + " dán văn bản" + (chars == null ? "" : " (" + chars + " ký tự)") + " khi đang thi";
		case WINDOW_BLUR -> name + " rời khỏi cửa sổ làm bài khi đang thi";
		default -> name + " rời khỏi trang làm bài khi đang thi";
		};
		notifications.notify(List.of(a.getTeacher().getId()), new NewNotification(NotificationType.EXAM_ALERT, title,
				a.getTitle() + " – " + s.getPart().getTitle()
						+ (reportedLate ? ". Xảy ra lúc mất mạng, máy sinh viên báo lại khi có mạng." : "")
						+ ". Xem chi tiết ở màn giám sát.",
				monitorLink(a)));
	}

	private static String monitorLink(Assignment a) {
		return "/giao-vien/bai-tap/" + a.getId() + "/giam-sat";
	}
}
