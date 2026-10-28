package com.ltmtu.ielts.exam;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

/**
 * Đẩy sự kiện thi tới {@code /topic/assignments/{id}/exam} <b>sau commit</b> (chỉ đẩy tin, không ghi DB) — rollback
 * thì không có tin. Kênh chỉ giáo viên chủ bài nghe được ({@link ExamChannelRule}).
 */
@Component
@RequiredArgsConstructor
class ExamPushListener {

	private final SimpMessagingTemplate messaging;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onRecorded(ExamEvents.Recorded e) {
		messaging.convertAndSend(topic(e.assignmentId()), e.push());
	}

	static String topic(Long assignmentId) {
		return "/topic/assignments/" + assignmentId + "/exam";
	}
}
