package com.ltmtu.ielts.stats.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.stats.dto.ProgressDtos.StudentRef;

/**
 * Thống kê một lớp (UC-19, FR-65) — docs/05-api.md mục A.6. Tính trên <b>thành viên hiện tại</b> của lớp và các bài
 * (đã phát hành / đã đóng) giao cho lớp; chỉ kết quả đã trả (BR-13).
 */
public final class ClassStatsDtos {

	private ClassStatsDtos() {
	}

	/** {@code averageBand}: trung bình band cả bài của mọi (thành viên, bài) đã đủ phần. */
	public record ClassStats(ClassInfo classroom, BigDecimal averageBand, List<ClassAssignment> assignments,
			List<RankingRow> ranking) {
	}

	public record ClassInfo(Long id, String code, String name, long memberCount) {
	}

	/**
	 * Một bài giao cho lớp. {@code completedCount}: thành viên đã nộp (ít nhất một lần) mọi phần; {@code gradedCount}:
	 * thành viên đã được trả mọi phần — {@code averageBand} tính trên những người này.
	 */
	public record ClassAssignment(Long assignmentId, String title, AssignmentStatus status, Instant dueAt,
			int partCount, int completedCount, double completedRate, int gradedCount, BigDecimal averageBand) {
	}

	/**
	 * Một hàng bảng xếp hạng. {@code averageBand}: trung bình band cả bài của các bài đã đủ phần
	 * ({@code gradedAssignments}). {@code assignmentBands}: band cả bài theo mã bài, null nếu chưa được trả đủ mọi
	 * phần. Xếp hạng kiểu thi đấu (1, 1, 3); chưa có band → {@code rank = null}, xếp cuối.
	 */
	public record RankingRow(Integer rank, StudentRef student, BigDecimal averageBand, int gradedAssignments,
			int submittedParts, int lateParts, Map<Long, BigDecimal> assignmentBands) {
	}
}
