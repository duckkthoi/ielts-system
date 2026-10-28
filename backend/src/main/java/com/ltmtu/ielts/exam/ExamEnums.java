package com.ltmtu.ielts.exam;

import java.util.Set;

/** Các enum của chế độ thi — khớp CHECK trong DB (V14, V15), docs/06 mục 8. */
public final class ExamEnums {

	private ExamEnums() {
	}

	public enum ExamEventType {
		STARTED, TAB_HIDDEN, TAB_VISIBLE, WINDOW_BLUR, WINDOW_FOCUS, PASTE, DISCONNECT, RECONNECT, OVERTIME, SUBMITTED;

		/** Client chỉ được tự báo các loại này; còn lại do server ghi (bắt đầu, nộp, mất kết nối, hết giờ). */
		private static final Set<ExamEventType> FROM_CLIENT = Set.of(TAB_HIDDEN, TAB_VISIBLE, WINDOW_BLUR, WINDOW_FOCUS,
				PASTE);

		public boolean fromClient() {
			return FROM_CLIENT.contains(this);
		}

		/** BR-25: rời trang / cửa sổ làm bài và dán văn bản thì báo giáo viên (có giới hạn tần suất). */
		public boolean alertsTeacher() {
			return leavesPage() || this == PASTE;
		}

		/** Rời khỏi bài: ẩn tab, hoặc tab vẫn hiện nhưng cửa sổ mất focus (Alt+Tab, màn hình thứ 2). */
		public boolean leavesPage() {
			return this == TAB_HIDDEN || this == WINDOW_BLUR;
		}
	}

	/** Trạng thái hiển thị của một phiên thi, tính từ giờ server (không lưu DB). */
	public enum ExamStatus {
		/** Đang làm, còn giờ. */
		IN_PROGRESS,
		/** Đã hết giờ mà chưa nộp — vẫn làm tiếp được (BR-24). */
		OVERTIME,
		/** Đã nộp trong giờ. */
		FINISHED,
		/** Đã nộp nhưng sau khi hết giờ. */
		FINISHED_OVERTIME
	}
}
