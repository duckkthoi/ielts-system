package com.ltmtu.ielts.assignment;

/** Các enum của module bài tập — khớp với ràng buộc CHECK trong DB (V1, V4, V16). */
public final class AssignmentEnums {

	private AssignmentEnums() {
	}

	/** docs/06-trang-thai.md mục 1. */
	public enum AssignmentStatus {
		DRAFT, PUBLISHED, CLOSED
	}

	public enum TaskType {
		WRITING_TASK1, WRITING_TASK2, CUSTOM;

		/** Trọng số mặc định khi gộp band (BR-12): Task 2 gấp đôi Task 1. */
		public java.math.BigDecimal defaultWeight() {
			return this == WRITING_TASK2 ? java.math.BigDecimal.valueOf(2) : java.math.BigDecimal.ONE;
		}
	}

	/** Mã tiêu chí IELTS; tiêu chí tự định nghĩa không có mã. */
	public enum CriterionCode {
		TA, TR, CC, LR, GRA
	}

	/**
	 * Loại bài (V16): ESSAY = bài luận IELTS (chấm band, cá nhân); PAPER = paper học thuật theo nhóm, chấm thang 10
	 * (docs/12).
	 */
	public enum AssignmentKind {
		ESSAY, PAPER
	}

	/** Kiểu trích dẫn của bài PAPER (BR-30). */
	public enum CitationStyle {
		IEEE, APA
	}

	/** BR-16: CHECK = đặc điểm cần có; LIMIT = đặc điểm tiêu cực giới hạn band. */
	public enum ChecklistKind {
		CHECK, LIMIT
	}
}
