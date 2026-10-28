package com.ltmtu.ielts.exam;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * UC-31 (đổi theo yêu cầu: <b>không tự nộp</b>): phiên thi hết giờ mà chưa nộp thì ghi sự kiện {@code OVERTIME} và
 * báo giáo viên + sinh viên đúng một lần (BR-24). Mỗi phiên một transaction; nhận việc bằng
 * {@code UPDATE … WHERE overtime_notified_at IS NULL} nên các lượt job chạy chồng nhau không báo trùng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamOvertimeJob {

	private final ExamSessionRepository sessionRepository;
	private final ExamEventService eventService;
	private final TransactionTemplate tx;
	private final Clock clock;

	/** Trả về số phiên vừa được báo quá giờ. */
	public int run() {
		Instant now = clock.instant();
		int flagged = 0;
		for (Long id : sessionRepository.findOverdueIds(now)) {
			Boolean done = tx.execute(status -> {
				if (sessionRepository.claimOvertime(id, now) != 1) {
					return false;
				}
				eventService.overtime(sessionRepository.findFull(id).orElseThrow(), now);
				return true;
			});
			if (Boolean.TRUE.equals(done)) {
				flagged++;
			}
		}
		if (flagged > 0) {
			log.info("Báo quá giờ {} phiên thi", flagged);
		}
		return flagged;
	}
}
