package com.ltmtu.ielts.grading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.CitationStyle;
import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;
import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.LevelDto;
import com.ltmtu.ielts.grading.GradingEnums.CriterionGroup;
import com.ltmtu.ielts.grading.GradingEnums.EditOp;
import com.ltmtu.ielts.grading.GradingEnums.GradeStatus;
import com.ltmtu.ielts.grading.GradingEnums.InlineKind;
import com.ltmtu.ielts.grading.GradingEnums.Resolution;
import com.ltmtu.ielts.grading.GradingEnums.ZeroReason;
import com.ltmtu.ielts.submission.SubmissionStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/** Chấm bài phía giáo viên — docs/05-api.md mục A.7 (UC-17, UC-18; SD-08). */
public final class GradingDtos {

	private GradingDtos() {
	}

	// ─── Hàng đợi chấm ───────────────────────────────────────────────────────

	/**
	 * {@code band}: điểm phần đã làm tròn để hiển thị (bài luận band IELTS, bài PAPER thang 10 — 2 chữ số); null khi
	 * chưa chấm hoặc chưa đủ tiêu chí. {@code groupName}: bài PAPER — tên nhóm; bài luận null.
	 */
	public record QueueRow(Long submissionId, SubmissionStatus status, int currentVersion, StudentInfo student,
			Long assignmentId, String assignmentTitle, Long partId, String partTitle, int partOrderNo,
			TaskType taskType, Instant submittedAt, boolean late, int wordCount, GradeStatus gradeStatus,
			BigDecimal band, AssignmentKind kind, String groupName) {
	}

	// ─── Màn chấm ────────────────────────────────────────────────────────────

	public record StudentInfo(Long id, String studentCode, String fullName) {
	}

	/** {@code revisionsUsed} = số lần đã yêu cầu sửa (= currentVersion − 1, docs/06 mục 2). */
	public record SubmissionInfo(Long id, SubmissionStatus status, int currentVersion, int revisionsUsed,
			StudentInfo student, String revisionNote, Instant revisionDueAt) {
	}

	/**
	 * {@code kind = PAPER}: chấm thang 10 theo mức đạt (BR-31), không áp quy tắc riêng của IELTS;
	 * {@code citationStyle}: kiểu trích dẫn để dựng bài nộp có định dạng (bài luận null).
	 */
	public record AssignmentInfo(Long id, String title, AssignmentStatus status, int maxRevisions,
			boolean showSuggestionsOnRevision, AssignmentKind kind, CitationStyle citationStyle) {
	}

	public record PartInfo(Long id, int orderNo, String title, TaskType taskType, String prompt, String imageUrl,
			Integer minWords, BigDecimal weight) {
	}

	/**
	 * {@code contentJson} / {@code references}: bài PAPER (B25c-2) — JSON trình soạn và tài liệu được trích lúc nộp
	 * ({@code [{id, csl}]}); bài luận để null.
	 */
	public record VersionInfo(int versionNo, String content, int wordCount, Instant submittedAt, boolean late,
			JsonNode contentJson, JsonNode references) {
	}

	/** Cảnh báo trên màn chấm: {@code SHORT_ESSAY} (≤ 20 từ, BR-18), {@code UNDER_MIN_WORDS}. */
	public record Warning(String code, String message) {
	}

	public record ChecklistInfo(Long id, String content, ChecklistKind kind, BigDecimal maxBand) {
	}

	/** {@code group}: nhóm tiêu chí để khớp mã lỗi / nhận xét mẫu (TA, TR → TASK). */
	public record CriterionInfo(Long id, CriterionCode code, CriterionGroup group, String name, String description,
			BigDecimal maxScore, BigDecimal step, BigDecimal weight, List<ChecklistInfo> checklist,
			List<LevelDto> levels) {
	}

	/** {@code maxAllowed} / {@code limitReason}: giới hạn điểm hiện tại của tiêu chí (BR-17, BR-18). */
	/** {@code levelId}: mức đạt đã chọn (bài PAPER), {@code null} = chấm điểm trực tiếp. */
	public record ScoreDto(Long criterionId, BigDecimal score, String comment, BigDecimal maxAllowed,
			String limitReason, Long levelId) {
	}

	/** Cộng / trừ riêng một thành viên (bài PAPER): {@code score} = điểm của người đó sau cộng / trừ, kẹp [0, 10]. */
	public record AdjustmentDto(Long studentId, String studentName, BigDecimal delta, String reason, BigDecimal score) {
	}

	public record MarkDto(Long checklistItemId, boolean checked) {
	}

	/** {@code resolution}: giáo viên đánh giá lại lỗi khi so sánh với lần nộp sau (null = tự đoán). */
	public record InlineCommentDto(Long id, int startOffset, int endOffset, String quote, String comment,
			String errorCode, Long criterionId, String suggestion, InlineKind kind, EditOp edit, Resolution resolution) {
	}

	/** Đặt / bỏ ({@code null}) đánh giá "đã sửa" cho một lỗi của lần nộp cũ. */
	public record ResolutionRequest(Resolution resolution) {
	}

	/**
	 * {@code rawBand}: band phần chưa làm tròn; {@code band}: đã làm tròn để hiển thị (BR-12). {@code complete}: đã
	 * chấm đủ mọi tiêu chí — điều kiện để trả bài.
	 */
	/**
	 * {@code band}: điểm hiển thị — bài luận theo band IELTS (BR-12), bài PAPER thang 10 làm tròn 2 chữ số (BR-31).
	 * {@code adjustments}: chỉ bài PAPER.
	 */
	public record GradeDto(Long id, int versionNo, GradeStatus status, ZeroReason zeroReason, String generalComment,
			BigDecimal rawBand, BigDecimal band, boolean complete, String graderName, Instant updatedAt,
			Instant returnedAt, List<ScoreDto> scores, List<MarkDto> checklist, List<InlineCommentDto> inlineComments,
			List<AdjustmentDto> adjustments) {
	}

	public record VersionSummary(int versionNo, Instant submittedAt, int wordCount, boolean late,
			GradeStatus gradeStatus, BigDecimal band) {
	}

	/**
	 * Dữ liệu màn chấm của một version. {@code editable}: version mới nhất và chưa trả bài. {@code canReturn}: sửa
	 * được và đã chấm đủ. {@code canRequestRevision}: trả được, còn lượt sửa (BR-10), bài chưa đóng.
	 */
	/** {@code members}: bài PAPER — thành viên hiện tại của nhóm (để cộng / trừ riêng); bài luận → rỗng. */
	public record GradingScreen(SubmissionInfo submission, AssignmentInfo assignment, PartInfo part,
			VersionInfo version, List<Warning> warnings, List<CriterionInfo> criteria, GradeDto grade,
			List<VersionSummary> versions, boolean editable, boolean canReturn, boolean canRequestRevision,
			List<StudentInfo> members) {
	}

	// ─── Lưu chấm ────────────────────────────────────────────────────────────

	/** {@code levelId}: bài PAPER — mức đạt đã chọn; điểm phải nằm trong khoảng của mức. */
	public record ScoreRequest(
			@NotNull(message = "Thiếu tiêu chí") Long criterionId,
			BigDecimal score,
			@Size(max = 2000, message = "Nhận xét tiêu chí tối đa 2000 ký tự") String comment,
			Long levelId) {
	}

	/** Cộng / trừ riêng một thành viên nhóm (bài PAPER): -2…+2, bước 0,25, bắt buộc lý do. */
	public record AdjustmentRequest(
			@NotNull(message = "Thiếu sinh viên") Long studentId,
			@NotNull(message = "Thiếu điểm cộng / trừ") BigDecimal delta,
			@Size(max = 500, message = "Lý do tối đa 500 ký tự") String reason) {
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
			@Size(max = 1000, message = "Gợi ý sửa tối đa 1000 ký tự") String suggestion,
			InlineKind kind,
			EditOp edit) {
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
			ZeroReason zeroReason,
			@Valid @Size(max = 10, message = "Tối đa 10 thành viên") List<AdjustmentRequest> memberAdjustments) {
	}

	public record RevisionRequest(
			@Size(max = 2000, message = "Lời nhắn tối đa 2000 ký tự") String note,
			@NotNull(message = "Vui lòng chọn hạn sửa") Instant dueAt) {
	}
}
