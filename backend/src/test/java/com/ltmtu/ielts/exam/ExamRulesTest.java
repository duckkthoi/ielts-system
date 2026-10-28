package com.ltmtu.ielts.exam;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;

import com.ltmtu.ielts.exam.ExamEnums.ExamStatus;

class ExamRulesTest {

	private static final Instant ENDS = Instant.parse("2026-11-05T10:40:00Z");

	@ParameterizedTest(name = "nộp {0}, giờ {1} → {2}")
	@DisplayName("BR-24: trạng thái phiên thi theo giờ kết thúc, lúc nộp và giờ hiện tại")
	@CsvSource(nullValues = "-", value = {
			"-,                    2026-11-05T10:39:59Z, IN_PROGRESS",
			"-,                    2026-11-05T10:40:00Z, OVERTIME",
			"-,                    2026-11-05T11:00:00Z, OVERTIME",
			"2026-11-05T10:30:00Z, 2026-11-05T11:00:00Z, FINISHED",
			"2026-11-05T10:40:00Z, 2026-11-05T11:00:00Z, FINISHED",
			"2026-11-05T10:40:01Z, 2026-11-05T11:00:00Z, FINISHED_OVERTIME" })
	void status(String finishedAt, String now, ExamStatus expected) {
		ExamStatus status = ExamRules.status(ENDS, finishedAt == null ? null : Instant.parse(finishedAt),
				Instant.parse(now));
		assertThat(status).isEqualTo(expected);
		assertThat(ExamRules.isOvertime(status))
				.isEqualTo(expected == ExamStatus.OVERTIME || expected == ExamStatus.FINISHED_OVERTIME);
	}

	@ParameterizedTest(name = "client báo {0} → ghi {1}, gửi bù = {2}")
	@DisplayName("BR-25: sự kiện gửi bù giữ giờ client báo (kẹp từ lúc bắt đầu); lệch < 5 giây hoặc không có giờ → giờ nhận")
	@CsvSource(nullValues = "-", value = {
			"-,                    2026-11-05T10:20:00Z, false",
			"2026-11-05T10:19:57Z, 2026-11-05T10:20:00Z, false",
			"2026-11-05T10:21:00Z, 2026-11-05T10:20:00Z, false",
			"2026-11-05T10:12:30Z, 2026-11-05T10:12:30Z, true",
			"2026-11-05T09:50:00Z, 2026-11-05T10:00:00Z, true" })
	void reportedAt(String clientAt, String expectedAt, boolean late) {
		Instant started = Instant.parse("2026-11-05T10:00:00Z");
		Instant now = Instant.parse("2026-11-05T10:20:00Z");
		ExamRules.Reported r = ExamRules.reportedAt(clientAt == null ? null : Instant.parse(clientAt), started, now);
		assertThat(r.at()).isEqualTo(Instant.parse(expectedAt));
		assertThat(r.late()).isEqualTo(late);
	}
}
