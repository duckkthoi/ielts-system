package com.ltmtu.ielts.paper;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ltmtu.ielts.paper.dto.PaperLiveCommentDtos.CommentPush;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class PaperLiveCommentPushListener {

	private final SimpMessagingTemplate messaging;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void push(PaperLiveCommentEvents.Changed e) {
		messaging.convertAndSend("/topic/paper/" + e.documentId() + "/comments",
				new CommentPush(e.type(), e.comment(), e.commentId()));
	}
}
