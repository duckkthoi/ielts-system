package com.ltmtu.ielts.exam;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Bộ lập lịch của chế độ thi. Chỉ gọi job, không chứa logic. Tắt cùng các job khác bằng
 * {@code app.reminders.enabled=false} (profile test tự gọi {@link ExamOvertimeJob#run()} với đồng hồ giả).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.reminders", name = "enabled", havingValue = "true", matchIfMissing = true)
class ExamScheduler {

	private final ExamOvertimeJob overtimeJob;

	@Scheduled(fixedDelayString = "${app.exam.overtime-check:PT15S}", initialDelayString = "PT20S")
	void flagOvertime() {
		overtimeJob.run();
	}
}
