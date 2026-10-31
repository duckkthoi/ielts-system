package com.ltmtu.ielts.stats.dto;

import java.math.BigDecimal;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;
import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;

/**
 * Thống kê một bài tập (UC-19, FR-63) — docs/05-api.md mục A.6. Chỉ tính kết quả <b>đã trả</b>; trung bình giữ 2 chữ
 * số thập phân (không làm tròn kiểu IELTS); {@code null} khi chưa có dữ liệu.
 */
public final class AssignmentStatsDtos {

	private AssignmentStatsDtos() {
	}

	public record AssignmentStats(int students, List<PartStats> perPart, OverallStats overall,
			List<ErrorCount> topErrors) {
	}

	/**
	 * {@code submittedCount}: đã nộp ít nhất một lần; {@code lateCount}: lần nộp đầu tiên bị muộn (BR-08);
	 * {@code onTimeRate} = (đã nộp − muộn) / đã nộp; {@code returnedCount}: có kết quả đã trả.
	 */
	public record PartStats(Long partId, int orderNo, String title, TaskType taskType, int submittedCount,
			int lateCount, Double onTimeRate, int returnedCount, BigDecimal averageBand, List<CriterionAverage> criteria,
			List<BandCount> bandDistribution) {
	}

	/** {@code maxScore}: điểm tối đa của tiêu chí (IELTS 9; bài PAPER theo rubric, thường 10). */
	public record CriterionAverage(Long criterionId, CriterionCode code, String name, BigDecimal maxScore,
			BigDecimal average, int count) {
	}

	/** Số bài theo band đã làm tròn hiển thị (bước 0.5), chỉ các band có ít nhất một bài, band tăng dần. */
	public record BandCount(BigDecimal band, int count) {
	}

	/** Band cả bài (BR-12): chỉ sinh viên đã được trả mọi phần. */
	public record OverallStats(int count, BigDecimal averageBand, List<BandCount> bandDistribution) {
	}

	/** Mã lỗi trong nhận xét inline (bỏ mã khen). {@code name} / {@code criterion} null nếu mã riêng đã bị xoá. */
	public record ErrorCount(String code, String name, String criterion, int count, int studentCount) {
	}
}
