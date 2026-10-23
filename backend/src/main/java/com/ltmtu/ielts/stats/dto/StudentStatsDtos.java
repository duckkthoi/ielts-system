package com.ltmtu.ielts.stats.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;
import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.stats.dto.AssignmentStatsDtos.ErrorCount;
import com.ltmtu.ielts.stats.dto.ProgressDtos.StudentRef;

/**
 * Kết quả học tập của một sinh viên (UC-28, FR-64) — docs/05-api.md mục A.6, A.8. Chỉ tính kết quả <b>đã trả</b>
 * (BR-13). Band hiển thị đã làm tròn .25 / .75; trung bình giữ 2 chữ số thập phân.
 */
public final class StudentStatsDtos {

	private StudentStatsDtos() {
	}

	/**
	 * {@code submittedCount}: số phần đã nộp ít nhất một lần; {@code lateCount}: số phần có lần nộp đầu muộn (BR-08);
	 * {@code returnedCount}: số phần có kết quả đã trả; {@code averageBand}: trung bình band cả bài của các bài đã đủ
	 * phần.
	 */
	public record StudentStats(StudentRef student, int submittedCount, int lateCount, int returnedCount,
			BigDecimal averageBand, List<AssignmentBand> assignments, List<PartBand> parts,
			List<CriterionCodeAverage> criteria, List<ErrorCount> errorProfile) {
	}

	/**
	 * Một bài có ít nhất một phần đã trả. {@code band} (BR-12) chỉ có khi mọi phần đã trả; {@code returnedAt} = lần
	 * trả gần nhất trong các phần. Sắp theo {@code returnedAt} tăng dần (biểu đồ band theo thời gian).
	 */
	public record AssignmentBand(Long assignmentId, String title, int partCount, int returnedParts, boolean complete,
			BigDecimal band, Instant returnedAt) {
	}

	/** Band của một phần đã trả, sắp theo thời điểm trả. */
	public record PartBand(Long assignmentId, String assignmentTitle, Long partId, String partTitle, TaskType taskType,
			BigDecimal band, Instant returnedAt) {
	}

	/** Trung bình điểm theo tiêu chí IELTS (gộp mọi bài); tiêu chí tự định nghĩa (không có mã) không tính. */
	public record CriterionCodeAverage(CriterionCode code, BigDecimal average, int count) {
	}
}
