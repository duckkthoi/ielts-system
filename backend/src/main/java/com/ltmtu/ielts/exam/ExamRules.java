package com.ltmtu.ielts.exam;

import java.time.Duration;
import java.time.Instant;

import com.ltmtu.ielts.exam.ExamEnums.ExamStatus;

/** Quy tắc thời gian của phiên thi (BR-23, BR-24). Class thuần, không phụ thuộc Spring. */
public final class ExamRules {

	private ExamRules() {
	}

	/**
	 * Trạng thái phiên thi. Tới đúng {@code endsAt} là đã hết giờ; nhưng nộp đúng {@code endsAt} vẫn tính trong giờ
	 * (giống hạn nộp: nộp lúc 23:59:00 với hạn 23:59 chưa muộn).
	 */
	public static ExamStatus status(Instant endsAt, Instant finishedAt, Instant now) {
		if (finishedAt != null) {
			return finishedAt.isAfter(endsAt) ? ExamStatus.FINISHED_OVERTIME : ExamStatus.FINISHED;
		}
		return now.isBefore(endsAt) ? ExamStatus.IN_PROGRESS : ExamStatus.OVERTIME;
	}

	/** Chênh lệch giờ client báo so với lúc nhận dưới mức này coi như gửi ngay (độ trễ mạng, sai số đồng hồ). */
	public static final Duration LATE_REPORT_THRESHOLD = Duration.ofSeconds(5);

	/** Thời điểm ghi cho một sự kiện client báo, và có phải gửi bù không. */
	public record Reported(Instant at, boolean late) {
	}

	/**
	 * Sự kiện client gửi bù khi có mạng lại mang giờ xảy ra (client đã quy về giờ server). Không tin hẳn giờ đó: kẹp
	 * trong {@code [startedAt, now]}; lệch dưới {@link #LATE_REPORT_THRESHOLD} hoặc không gửi giờ → dùng giờ nhận.
	 */
	public static Reported reportedAt(Instant clientAt, Instant startedAt, Instant now) {
		if (clientAt == null || !clientAt.isBefore(now.minus(LATE_REPORT_THRESHOLD))) {
			return new Reported(now, false);
		}
		return new Reported(clientAt.isBefore(startedAt) ? startedAt : clientAt, true);
	}

	public static boolean isOvertime(ExamStatus status) {
		return status == ExamStatus.OVERTIME || status == ExamStatus.FINISHED_OVERTIME;
	}
}
