package com.ltmtu.ielts.paper;

import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.UpdatePush;

/** Sự kiện nội bộ của module paper. */
final class PaperEvents {

	private PaperEvents() {
	}

	/** Một update Yjs vừa được lưu; sau commit phát tới {@code /topic/paper/{id}}. */
	record UpdateSaved(Long documentId, UpdatePush push) {
	}
}
