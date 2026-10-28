package com.ltmtu.ielts.paper;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

/**
 * Phát update Yjs tới {@code /topic/paper/{id}} <b>sau commit</b>: client chỉ thấy update đã có số thứ tự trong DB, nên
 * lấy bù bằng {@code afterSeq} không bao giờ bỏ sót. Người gửi cũng nhận lại (biết update đã được lưu qua {@code clientId}).
 */
@Component
@RequiredArgsConstructor
class PaperPushListener {

	private final SimpMessagingTemplate messaging;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onUpdateSaved(PaperEvents.UpdateSaved e) {
		messaging.convertAndSend(PaperDocumentService.topic(e.documentId()), e.push());
	}
}
