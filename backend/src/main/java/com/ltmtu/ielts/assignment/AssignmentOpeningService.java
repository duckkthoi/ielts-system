package com.ltmtu.ielts.assignment;

import java.time.Instant;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Bài phát hành với giờ mở ở tương lai (BR-05): khi tới giờ mở mới thông báo "Bài tập mới" (SD-05 ghi chú). Được bộ
 * lập lịch gọi định kỳ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentOpeningService {

	private final AssignmentRepository assignmentRepository;
	private final AssignmentTargetRepository targetRepository;
	private final ApplicationEventPublisher events;

	/**
	 * Phát {@link AssignmentEvents.Opened} cho mỗi bài vừa tới giờ mở. Mỗi bài được "giành" bằng một câu UPDATE có điều
	 * kiện, nên job chạy chồng nhau cũng chỉ thông báo một lần. Người nhận tính tại thời điểm mở (gồm cả người được giao
	 * thêm trước giờ mở). Trả về số bài đã thông báo.
	 */
	@Transactional
	public int announceOpened(Instant now) {
		int announced = 0;
		for (Assignment a : assignmentRepository.findOpenedNotAnnounced(now)) {
			if (assignmentRepository.claimOpenNotification(a.getId(), now) == 0) {
				continue;
			}
			Set<Long> recipients = targetRepository.findRecipientIds(a.getId());
			events.publishEvent(new AssignmentEvents.Opened(a.getId(), a.getTitle(), a.getDueAt(), Set.copyOf(recipients)));
			log.info("Bài tập {} tới giờ mở, thông báo cho {} sinh viên", a.getId(), recipients.size());
			announced++;
		}
		return announced;
	}
}
