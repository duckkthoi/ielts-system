package com.ltmtu.ielts.grading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;
import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.grading.GradingEnums.CriterionGroup;
import com.ltmtu.ielts.grading.GradingEnums.GradeStatus;
import com.ltmtu.ielts.grading.GradingEnums.ZeroReason;
import com.ltmtu.ielts.submission.SubmissionStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Chấm bài phía giáo viên — docs/05-api.md mục A.7 (UC-17, UC-18; SD-08). */
public final class GradingDtos {

	private GradingDtos() {
	}

	// ─── Hàng đợi chấm ───────────────────────────────────────────────────────

	/** {@code band}: band phần đã làm tròn để hiển thị; null khi chưa chấm hoặc chưa đủ tiêu chí. */
	public record QueueRow(Long submissionId, SubmissionStatus status, int currentVersion, StudentInfo student,
			Long assignmentId, String assignmentTitle, Long partId, String partTitle, int partOrderNo,
			TaskType taskType, Instant submittedAt, boolean late, int wordCount, GradeStatus gradeStatus,
			BigDecimal band) {
	}

	// ─── Màn chấm ────────────────────────────────────────────────────────────

	public record StudentInfo(Long id, String studentCode, String fullName) {
	}

	/** {@code revisionsUsed} = số lần đã yêu cầu sửa (= currentVersion − 1, docs/06 mục 2). */
	public record SubmissionInfo(Long id, SubmissionStatus status, int currentVersion, int revisionsUsed,
			StudentInfo student, String revisionNote, Instant revisionDueAt) {
	}

	public record AssignmentInfo(Long id, String title, AssignmentStatus status, int maxRevisions,
			boolean showSuggestionsOnRevision) {
	}

	public record PartInfo(Long id, int orderNo, String title, TaskType taskType, String prompt, String imageUrl,
			Integer minWords, BigDecimal weight) {
	}

	public record VersionInfo(int versionNo, String content, int wordCount, Instant submittedAt, boolean late) {
	}

	/** Cảnh báo trên màn chấm: {@code SHORT_ESSAY} (≤ 20 từ, BR-18), {@code UNDER_MIN_WORDS}. */
	public record Warning(String code, String message) {
	}

	public record ChecklistInfo(Long id, String content, ChecklistKind kind, BigDecimal maxBand) {
	}

	/** {@code group}: nhóm tiêu chí để khớp mã lỗi / nhận xét mẫu (TA, TR → TASK). */
	public record CriterionInfo(Long id, CriterionCode code, CriterionGroup group, String name, String description,
			BigDecimal maxScore, BigDecimal step, BigDecimal weight, List<ChecklistInfo> checklist) {
	}

	/** {@code maxAllowed} / {@code limitReason}: giới hạn điểm hiện tại của tiêu chí (BR-17, BR-18). */
	public record ScoreDto(Long criterionId, BigDecimal score, String comment, BigDecimal maxAllowed,
			String limitReason) {
	}

	public record MarkDto(Long checklistItemId, boolean checked) {
	}

	public record InlineCommentDto(Long id, int startOffset, int endOffset, String quote, String comment,
			String errorCode, Long criterionId, String suggestion) {
	}

	/**
	 * {@code rawBand}: band phần chưa làm tròn; {@code band}: đã làm tròn để hiển thị (BR-12). {@code complete}: đã
	 * chấm đủ mọi tiêu chí — điều kiện để trả bài.
	 */
	public record GradeDto(Long id, int versionNo, GradeStatus status, ZeroReason zeroReason, String generalComment,
			BigDecimal rawBand, BigDecimal band, boolean complete, String graderName, Instant updatedAt,
			Instant returnedAt, List<ScoreDto> scores, List<MarkDto> checklist, List<InlineCommentDto> inlineComments) {
	}

	public record VersionSummary(int versionNo, Instant submittedAt, int wordCount, boolean late,
			GradeStatus gradeStatus, BigDecimal band) {
	}

	/**
	 * Dữ liệu màn chấm của một version. {@code editable}: version mới nhất và chưa trả bài. {@code canReturn}: sửa
	 * được và đã chấm đủ. {@code canRequestRevision}: trả được, còn lượt sửa (BR-10), bài chưa đóng.
	 */
	public record GradingScreen(SubmissionInfo submission, AssignmentInfo assignment, PartInfo part,
			VersionInfo version, List<Warning> warnings, List<CriterionInfo> criteria, GradeDto grade,
			List<VersionSummary> versions, boolean editable, boolean canReturn, boolean canRequestRevision) {
	}

	// ─── Lưu chấm ────────────────────────────────────────────────────────────

	public record ScoreRequest(
			@NotNull(message = "Thiếu tiêu chí") Long criterionId,
			BigDecimal score,
			@Size(max = 2000, message = "Nhận xét tiêu chí tối đa 2000 ký tự") String comment) {
	}

	public record MarkRequest(@NotNull(message = "Thiếu mục checklist") Long checklistItemId, Boolean checked) {
	}

	/** Offset tính theo đơn vị UTF-16 trên nội dung version (giống {@code String.length()} của Java và JavaScript). */
	public record InlineCommentRequest(
			@NotNull(message = "Thiếu vị trí bắt đầu") Integer startOffset,
			@NotNull(message = "Thiếu vị trí kết thúc") Integer endOffset,
			String quote,
			@Size(max = 2000, message = "Nhận xét inline tối đa 2000 ký tự") String comment,
			@Size(max = 20, message = "Mã lỗi tối đa 20 ký tự") String errorCode,
			Long criterionId,
			@Size(max = 1000, message = "Gợi ý sửa tối đa 1000 ký tự") String suggestion) {
	}

	/**
	 * PUT = thay toàn bộ kết quả chấm của version: tiêu chí không gửi thì coi là chưa chấm, mục checklist không gửi
	 * thì coi là chưa tick, nhận xét inline không gửi thì bị xoá.
	 */
	public record SaveGradeRequest(
			@NotNull(message = "Thiếu version đang chấm") Integer versionNo,
			@Valid List<ScoreRequest> scores,
			@Valid List<MarkRequest> checklist,
			@Valid @Size(max = 300, message = "Tối đa 300 nhận xét inline") List<InlineCommentRequest> inlineComments,
			@Size(max = 5000, message = "Nhận xét chung tối đa 5000 ký tự") String generalComment,
			ZeroReason zeroReason) {
	}

	public record RevisionRequest(
			@Size(max = 2000, message = "Lời nhắn tối đa 2000 ký tự") String note,
			@NotNull(message = "Vui lòng chọn hạn sửa") Instant dueAt) {
	}
}
