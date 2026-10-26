package com.ltmtu.ielts.grading;

import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;

/** Các enum của module chấm bài — khớp với ràng buộc CHECK trong DB (V1, V5). */
public final class GradingEnums {

	private GradingEnums() {
	}

	/** docs/06-trang-thai.md mục 3: DRAFT = sinh viên chưa thấy; RETURNED = đã công bố (BR-13). */
	public enum GradeStatus {
		DRAFT, RETURNED
	}

	/** BR-19: lý do bắt buộc khi "Chấm 0" (docs/08 mục 3.3). */
	public enum ZeroReason {
		/** Không làm bài. */
		NO_ATTEMPT,
		/** Viết hoàn toàn không bằng tiếng Anh. */
		NOT_ENGLISH,
		/** Có bằng chứng bài hoàn toàn học thuộc. */
		MEMORISED
	}

	/**
	 * Nhóm tiêu chí của mã lỗi và nhận xét mẫu. {@code TASK} gộp TA (Task 1) và TR (Task 2), để một mã như
	 * {@code IRR} dùng được cho cả hai loại Task.
	 */
	public enum CriterionGroup {
		TASK, CC, LR, GRA;

		public static CriterionGroup of(CriterionCode code) {
			if (code == null) {
				return null;
			}
			return switch (code) {
				case TA, TR -> TASK;
				case CC -> CC;
				case LR -> LR;
				case GRA -> GRA;
			};
		}
	}
}
