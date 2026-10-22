package com.ltmtu.ielts.notification;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.notification.NotificationService.NewNotification;
import com.ltmtu.ielts.notification.ReminderLog.Kind;
import com.ltmtu.ielts.notification.ReminderLogRepository.Candidate;

import lombok.extern.slf4j.Slf4j;

/**
 * Nhắc hạn nộp và hạn sửa tự động (UC-30, SD-10, FR-52, BR-14).
 * <p>
 * Chống nhắc trùng không dựa vào "kiểm tra trước": với từng người, chèn {@code reminder_log} bằng
 * {@code ON CONFLICT DO NOTHING}, chỉ ai chèn được mới nhận thông báo. Job chạy chồng nhau hay server khởi động lại giữa
 * chừng cũng không nhắc hai lần. Mỗi nhóm (phần, mốc) là một transaction: tạo thông báo lỗi thì bản ghi "đã nhắc" cũng
 * được huỷ, lần chạy sau nhắc lại.
 */
@Slf4j
@Service
public class DeadlineReminderJob {

	private final ReminderLogRepository reminderLogRepository;
	private final NotificationService notificationService;
	private final TransactionTemplate tx;
	private final Clock clock;
	private final AppProperties.Reminders config;
	private final DateTimeFormatter timeFormat;

	public DeadlineReminderJob(ReminderLogRepository reminderLogRepository, NotificationService notificationService,
			TransactionTemplate tx, Clock clock, AppProperties props) {
		this.reminderLogRepository = reminderLogRepository;
		this.notificationService = notificationService;
		this.tx = tx;
		this.clock = clock;
		this.config = props.reminders();
		this.timeFormat = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(props.zone());
	}

	private record GroupKey(Long partId, String kind) {
	}

	/** Chạy một lượt; trả về số sinh viên đã được nhắc ở lượt này. */
	public int run() {
		Instant now = clock.instant();
		List<Candidate> candidates = reminderLogRepository.findCandidates(now, now.plus(config.urgent()),
				now.plus(config.soon()));
		Map<GroupKey, List<Candidate>> groups = new LinkedHashMap<>();
		candidates.forEach(c -> groups.computeIfAbsent(new GroupKey(c.getPartId(), c.getKind()), k -> new ArrayList<>()).add(c));

		int reminded = 0;
		for (List<Candidate> group : groups.values()) {
			Integer n = tx.execute(status -> remindGroup(group, now));
			reminded += n == null ? 0 : n;
		}
		if (reminded > 0) {
			log.info("Nhắc hạn: {} sinh viên ({} nhóm phần / mốc)", reminded, groups.size());
		}
		return reminded;
	}

	private int remindGroup(List<Candidate> group, Instant now) {
		Candidate first = group.getFirst();
		List<Long> students = group.stream()
				.filter(c -> reminderLogRepository.insertIfAbsent(c.getPartId(), c.getStudentId(), c.getKind(), now) == 1)
				.map(Candidate::getStudentId)
				.toList();
		if (students.isEmpty()) {
			return 0;
		}
		notificationService.notify(students, message(first, Kind.valueOf(first.getKind())));
		return students.size();
	}

	private NewNotification message(Candidate c, Kind kind) {
		boolean urgent = kind == Kind.H2 || kind == Kind.REVISION_H2;
		String left = "còn dưới " + (urgent ? config.urgent() : config.soon()).toHours() + " giờ";
		String title = (kind.revision() ? "Sắp hết hạn sửa bài: " : "Sắp hết hạn nộp: ") + c.getAssignmentTitle()
				+ " – " + c.getPartTitle();
		String body = (kind.revision() ? "Hạn sửa: " : "Hạn nộp: ") + timeFormat.format(c.getDueAt()) + " (" + left
				+ "). " + (kind.revision() ? "Bạn chưa nộp bản sửa." : "Bạn chưa nộp phần này.");
		return new NewNotification(NotificationType.DEADLINE_REMINDER, title, body,
				"/sinh-vien/bai-tap/" + c.getAssignmentId());
	}
}
