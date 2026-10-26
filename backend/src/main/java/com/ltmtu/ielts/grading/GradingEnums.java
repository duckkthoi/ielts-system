package com.ltmtu.ielts.grading;

import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;

/** Các enum của module chấm bài — khớp với ràng buộc CHECK trong DB (V1, V5, V9, V10). */
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

	/** Kiểu đánh dấu trên bài (V9, V10): chỉ ERROR được tính vào thống kê lỗi. */
	public enum InlineKind {
		/** Lỗi — gạch chân gợn sóng theo loại lỗi; mang được sửa (track changes). */
		ERROR,
		/** Lưu ý — nền vàng. */
		HIGHLIGHT,
		/** Dùng hay — nền xanh nhạt. */
		GOOD,
		/** Chỉ nhận xét — nền xám nhạt (V10). */
		COMMENT
	}

	/** Sửa kiểu track changes (V10); chữ gốc của sinh viên không đổi. */
	public enum EditOp {
		/** Gạch chữ cũ, hiện chữ mới ngay cạnh. */
		REPLACE,
		/** Gạch chữ cũ. */
		DELETE,
		/** Chèn chữ mới ngay sau đoạn được chọn. */
		INSERT
	}

	/** Giáo viên đánh giá lại một lỗi của lần nộp cũ khi so sánh với lần nộp sau (V9). */
	public enum Resolution {
		/** Đã sửa. */
		FIXED,
		/** Chưa sửa. */
		NOT_FIXED,
		/** Đã đổi nhưng khác gợi ý. */
		CHANGED
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
