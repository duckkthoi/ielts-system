package com.ltmtu.ielts.exam;

import com.ltmtu.ielts.exam.dto.ExamDtos.ExamPush;

/** Sự kiện nội bộ của module thi. */
final class ExamEvents {

	private ExamEvents() {
	}

	/** Một sự kiện thi vừa được lưu; sau commit đẩy tới kênh giám sát của bài. */
	record Recorded(Long assignmentId, ExamPush push) {
	}
}
