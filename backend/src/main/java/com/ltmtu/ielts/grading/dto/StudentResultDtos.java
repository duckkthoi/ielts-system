package com.ltmtu.ielts.grading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;
import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.grading.GradingEnums.EditOp;
import com.ltmtu.ielts.grading.GradingEnums.InlineKind;
import com.ltmtu.ielts.grading.GradingEnums.Resolution;
import com.ltmtu.ielts.grading.GradingEnums.ZeroReason;

import tools.jackson.databind.JsonNode;

/**
 * Kết quả chấm phía sinh viên — docs/05-api.md mục A.8 (BR-13, docs/08 mục 4.5). Chỉ có dữ liệu khi kết quả đã được
 * trả ({@code RETURNED}); kết quả đang chấm nháp không bao giờ lộ ra.
 */
public final class StudentResultDtos {

	private StudentResultDtos() {
	}

	/** {@code band} / {@code returnedAt}: chỉ khác null khi version đó đã được trả bài. */
	public record VersionSummary(int versionNo, Instant submittedAt, int wordCount, boolean late, boolean returned,
			BigDecimal band, Instant returnedAt) {
	}

	/**
	 * {@code result}: null khi version chưa được trả bài (đang chờ chấm / đang chấm). Bài PAPER (B25a) có thêm
	 * {@code contentJson} (JSON trình soạn) và {@code references} ({@code [{id, csl}]} — tài liệu được trích lúc nộp);
	 * bài luận để null.
	 */
	public record VersionDetail(int versionNo, Instant submittedAt, int wordCount, boolean late, String content,
			JsonNode contentJson, JsonNode references, Result result) {
	}

	public record ChecklistMark(String content, ChecklistKind kind, BigDecimal maxBand, boolean checked) {
	}

	/** {@code limitedTo} / {@code limitReason}: tiêu chí bị giới hạn điểm vì mục nào (vd "CC bị giới hạn ở 5 vì: …"). */
	public record CriterionResult(Long criterionId, CriterionCode code, String name, BigDecimal maxScore,
			BigDecimal score, String comment, BigDecimal limitedTo, String limitReason, List<ChecklistMark> checklist) {
	}

	/**
	 * {@code suggestion}, {@code edit}: null khi bị ẩn (BR-21) hoặc giáo viên không ghi. {@code kind}: lỗi / lưu ý /
	 * dùng hay / nhận xét;
	 * {@code resolution}: giáo viên đánh giá lỗi này đã sửa ở lần nộp sau chưa (null = chưa đánh giá).
	 */
	public record StudentInlineComment(int startOffset, int endOffset, String quote, String comment, String errorCode,
			String errorName, CriterionCode criterionCode, String criterionName, String suggestion, InlineKind kind,
			EditOp edit, Resolution resolution) {
	}

	/** Một dòng bảng "Lỗi trong bài": mã lỗi và số lần xuất hiện, theo tiêu chí. Không tính mã khen. */
	public record ErrorCount(CriterionCode criterionCode, String criterionName, String errorCode, String errorName,
			int count) {
	}

	/**
	 * {@code suggestionsHidden}: gợi ý sửa đang bị ẩn vì bài đang "Cần sửa" (BR-21) — sinh viên tự sửa theo mã lỗi.
	 * Bài PAPER (B25e): {@code band} = điểm nhóm thang 10; {@code myScore} = điểm riêng của sinh viên (đã cộng / trừ),
	 * {@code myAdjustment} / {@code myAdjustmentReason} = mức và lý do cộng / trừ (null nếu không có); bài luận null.
	 */
	public record Result(BigDecimal band, ZeroReason zeroReason, String generalComment, String graderName,
			Instant returnedAt, boolean suggestionsHidden, List<CriterionResult> criteria,
			List<StudentInlineComment> inlineComments, List<ErrorCount> errorSummary, BigDecimal myScore,
			BigDecimal myAdjustment, String myAdjustmentReason) {
	}

	/** {@code versionNo} / {@code band}: lần nộp gần nhất đã được trả bài của phần; null nếu chưa có. */
	public record PartResult(Long partId, int orderNo, String title, TaskType taskType, BigDecimal weight,
			Integer versionNo, BigDecimal band) {
	}

	/**
	 * Band cả bài (BR-12): chỉ có khi mọi phần đã có kết quả; {@code note} ghi cách tính ước lượng hoặc "Chưa đủ
	 * phần".
	 */
	public record AssignmentResult(Long assignmentId, String title, List<PartResult> parts, boolean complete,
			BigDecimal band, String note) {
	}
}
