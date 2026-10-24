package com.ltmtu.ielts.stats.dto;

import java.math.BigDecimal;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.submission.SubmissionStatus;

/** Ma trận tiến độ của một bài tập (UC-16) — docs/05-api.md mục A.6. */
public final class ProgressDtos {

	private ProgressDtos() {
	}

	public record ProgressMatrix(List<PartColumn> parts, List<ProgressRow> rows, ProgressSummary summary) {
	}

	public record PartColumn(Long id, int orderNo, String title, TaskType taskType) {
	}

	public record StudentRef(Long id, String studentCode, String fullName) {
	}

	/**
	 * Một hàng = một sinh viên; {@code cells} theo đúng thứ tự {@code parts}. {@code online} / {@code viewingPartId}:
	 * đang có kết nối WebSocket / đang mở phần nào của bài này (Mức 2, FR-54) — cập nhật tiếp qua kênh presence.
	 */
	public record ProgressRow(StudentRef student, boolean online, Long viewingPartId, List<ProgressCell> cells) {
	}

	/**
	 * Một ô = một (sinh viên, phần). {@code late}: lần nộp mới nhất bị muộn (BR-08). {@code overdue}: còn phải nộp
	 * (chưa nộp / đang nháp / cần sửa) mà đã quá hạn áp dụng (BR-07). {@code score}: band của phần đã làm tròn hiển
	 * thị, chỉ khi kết quả của lần nộp mới nhất đã trả.
	 */
	public record ProgressCell(Long partId, Long submissionId, SubmissionStatus status, int currentVersion,
			boolean late, boolean overdue, BigDecimal score) {
	}

	/**
	 * Tóm tắt trên các sinh viên của ma trận (đã lọc lớp, chưa lọc trạng thái). Hoàn thành = đã nộp ít nhất một lần
	 * cho mọi phần; {@code lateCount} = số sinh viên có ít nhất một phần nộp muộn.
	 */
	public record ProgressSummary(int students, int completedCount, double completedRate, int lateCount,
			int overdueCount) {
	}
}
