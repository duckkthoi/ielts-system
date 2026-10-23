package com.ltmtu.ielts.realtime;

import java.time.Clock;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ltmtu.ielts.grading.GradingEvents;
import com.ltmtu.ielts.realtime.RealtimeDtos.ProgressEvent;
import com.ltmtu.ielts.submission.SubmissionEvents;
import com.ltmtu.ielts.submission.SubmissionStatus;

import lombok.RequiredArgsConstructor;

/**
 * Tiến độ live (FR-61): ô của ma trận đổi màu ngay khi sinh viên bắt đầu làm / nộp, giáo viên trả bài / yêu cầu sửa.
 * Đẩy tới {@code /topic/assignments/{id}/progress} <b>sau commit</b> (chỉ đẩy tin, không ghi DB) — rollback thì
 * không có tin. Kênh chỉ giáo viên chủ bài nghe được ({@link AssignmentChannelRule}).
 */
@Component
@RequiredArgsConstructor
public class ProgressPushListener {

	private final SimpMessagingTemplate messaging;
	private final Clock clock;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onDraftStarted(SubmissionEvents.DraftStarted e) {
		push(e.assignmentId(), new ProgressEvent(e.submissionId(), e.studentId(), e.partId(), SubmissionStatus.DRAFT,
				false, null, clock.instant()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSubmitted(SubmissionEvents.Submitted e) {
		push(e.assignmentId(), new ProgressEvent(e.submissionId(), e.studentId(), e.partId(), SubmissionStatus.SUBMITTED,
				e.late(), null, clock.instant()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onReturned(GradingEvents.Returned e) {
		push(e.assignmentId(), new ProgressEvent(e.submissionId(), e.studentId(), e.partId(), SubmissionStatus.RETURNED,
				false, e.band(), clock.instant()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onRevisionRequested(GradingEvents.RevisionRequested e) {
		push(e.assignmentId(), new ProgressEvent(e.submissionId(), e.studentId(), e.partId(),
				SubmissionStatus.REVISION_REQUESTED, false, e.band(), clock.instant()));
	}

	private void push(Long assignmentId, ProgressEvent event) {
		messaging.convertAndSend("/topic/assignments/" + assignmentId + "/progress", event);
	}
}
