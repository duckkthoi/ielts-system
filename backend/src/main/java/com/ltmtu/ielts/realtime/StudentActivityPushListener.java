package com.ltmtu.ielts.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

/** Đẩy hoạt động sau commit tới giáo viên chủ bài. */
@Component
@RequiredArgsConstructor
public class StudentActivityPushListener {

	private final SimpMessagingTemplate messaging;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onActivity(StudentActivityRecorded event) {
		String destination = "/topic/assignments/" + event.assignmentId() + "/activity";
		messaging.convertAndSend(destination, event.activity());
	}
}
