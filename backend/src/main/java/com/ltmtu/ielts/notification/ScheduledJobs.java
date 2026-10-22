package com.ltmtu.ielts.notification;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ltmtu.ielts.assignment.AssignmentOpeningService;

import lombok.RequiredArgsConstructor;

/**
 * Bộ lập lịch (tác nhân "Bộ lập lịch" ở UC-30). Chỉ gọi job, không chứa logic. Tắt bằng
 * {@code app.reminders.enabled=false} (profile test tự gọi job với đồng hồ giả).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.reminders", name = "enabled", havingValue = "true", matchIfMissing = true)
class ScheduledJobs {

	private final AssignmentOpeningService openingService;
	private final DeadlineReminderJob reminderJob;
	private final Clock clock;

	/** Bài phát hành trước giờ mở: thông báo khi tới giờ mở. */
	@Scheduled(fixedDelayString = "${app.reminders.interval:PT15M}", initialDelayString = "PT30S")
	void announceOpenedAssignments() {
		openingService.announceOpened(clock.instant());
	}

	/** SD-10: nhắc hạn nộp / hạn sửa. */
	@Scheduled(fixedDelayString = "${app.reminders.interval:PT15M}", initialDelayString = "PT45S")
	void remindDeadlines() {
		reminderJob.run();
	}
}
