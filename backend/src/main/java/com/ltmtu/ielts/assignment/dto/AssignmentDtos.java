package com.ltmtu.ielts.assignment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.CitationStyle;
import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;
import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO của nhóm API bài tập — docs/05-api.md mục A.5. */
public final class AssignmentDtos {

	private AssignmentDtos() {
	}

	// ─── Request ──────────────────────────────────────────────────────────────

	/** Tạo bài tập hoặc thay toàn bộ bài tập đang ở trạng thái nháp. */
	public record AssignmentRequest(
			@NotBlank(message = "Vui lòng nhập tên bài tập") @Size(max = 200) String title,
			@Size(max = 5000) String description,
			Instant openAt,
			Instant dueAt,
			Boolean allowLate,
			@Min(value = 0, message = "Số lần sửa không được âm") @Max(value = 10, message = "Tối đa 10 lần sửa")
			Integer maxRevisions,
			Boolean showSuggestionsOnRevision,
			@Size(max = 10, message = "Tối đa 10 phần mỗi bài") List<@Valid PartRequest> parts,
			AssignmentKind kind,
			CitationStyle citationStyle,
			@Min(value = 1, message = "Nhóm có từ 1 tới 10 thành viên")
			@Max(value = 10, message = "Nhóm có từ 1 tới 10 thành viên") Integer maxGroupSize,
			Instant groupLockAt) {
	}

	public record PartRequest(
			@NotBlank(message = "Vui lòng nhập tiêu đề phần") @Size(max = 200) String title,
			@NotNull(message = "Vui lòng chọn loại phần") TaskType taskType,
			@NotBlank(message = "Vui lòng nhập đề bài") @Size(max = 20000) String prompt,
			@Size(max = 500) String imageUrl,
			@Min(0) @Max(2000) Integer minWords,
			@Min(1) @Max(300) Integer suggestedMinutes,
			Instant partDueAt,
			@DecimalMin(value = "0.1", message = "Trọng số phải lớn hơn 0")
			@DecimalMax(value = "10", message = "Trọng số tối đa 10")
			BigDecimal weight,
			@Size(max = 10, message = "Tối đa 10 tiêu chí mỗi phần") List<@Valid CriterionRequest> criteria,
			Boolean examMode,
			@Min(value = 1, message = "Thời gian thi từ 1 tới 300 phút")
			@Max(value = 300, message = "Thời gian thi từ 1 tới 300 phút") Integer examDurationMinutes,
			@Size(max = 20, message = "Tối đa 20 mục mỗi phần") List<@Valid SectionRequest> sections) {
	}

	/** Một mục trong khung của giai đoạn bài PAPER. {@code sectionKey} bỏ trống → sinh từ tiêu đề. */
	public record SectionRequest(
			@Size(max = 40) String sectionKey,
			@NotBlank(message = "Vui lòng nhập tên mục") @Size(max = 200) String title,
			Boolean required,
			@Min(value = 0, message = "Số từ tối thiểu không được âm") Integer minWords,
			@Min(value = 1, message = "Số từ tối đa phải lớn hơn 0") Integer maxWords) {
	}

	public record CriterionRequest(
			CriterionCode code,
			@NotBlank(message = "Vui lòng nhập tên tiêu chí") @Size(max = 150) String name,
			@Size(max = 2000) String description,
			@DecimalMin(value = "1", message = "Điểm tối đa từ 1 tới 10") @DecimalMax(value = "10", message = "Điểm tối đa từ 1 tới 10")
			BigDecimal maxScore,
			BigDecimal step,
			@DecimalMin(value = "0.1", message = "Trọng số phải lớn hơn 0") @DecimalMax("10") BigDecimal weight,
			@Size(max = 30, message = "Tối đa 30 mục checklist mỗi tiêu chí") List<@Valid ChecklistItemRequest> checklist) {
	}

	public record ChecklistItemRequest(
			@NotBlank(message = "Nội dung mục checklist không được trống") @Size(max = 500) String content,
			ChecklistKind kind,
			BigDecimal maxBand) {
	}

	/**
	 * Sửa bài đã phát hành (BR-06): chỉ những thông tin không làm sai lệch bài đã nộp.
	 * Trường {@code null} = giữ nguyên. {@code partDueAts}: id phần → hạn riêng mới (giá trị null = bỏ hạn riêng).
	 */
	public record AssignmentSettingsRequest(
			@Size(max = 200) String title,
			@Size(max = 5000) String description,
			Instant dueAt,
			Boolean allowLate,
			@Min(0) @Max(10) Integer maxRevisions,
			Boolean showSuggestionsOnRevision,
			Map<Long, Instant> partDueAts,
			Instant groupLockAt,
			@Min(value = 1, message = "Nhóm có từ 1 tới 10 thành viên")
			@Max(value = 10, message = "Nhóm có từ 1 tới 10 thành viên") Integer maxGroupSize) {
	}

	// ─── Response ─────────────────────────────────────────────────────────────

	/** Một dòng danh sách bài tập đọc từ DB, chưa có số liệu nộp bài. */
	public record AssignmentRow(Long id, String title, AssignmentStatus status, Instant openAt, Instant dueAt,
			int partCount, Instant publishedAt, Instant createdAt, Instant updatedAt) {
	}

	/**
	 * Danh sách bài tập của giáo viên. {@code recipientCount}: số người nhận hiện tại; {@code completedCount}: số người
	 * nhận đã nộp đủ mọi phần; {@code pendingGradingCount}: số bài nộp (phần) đang chờ chấm.
	 */
	public record AssignmentSummary(Long id, String title, AssignmentStatus status, Instant openAt, Instant dueAt,
			int partCount, Instant publishedAt, Instant createdAt, Instant updatedAt, long recipientCount,
			long completedCount, long pendingGradingCount) {
	}

	public record AssignmentDetail(Long id, String title, String description, AssignmentStatus status,
			Instant openAt, Instant dueAt, boolean allowLate, int maxRevisions, boolean showSuggestionsOnRevision,
			Instant publishedAt, Instant createdAt, Instant updatedAt, List<PartDto> parts, AssignmentKind kind,
			CitationStyle citationStyle, Integer maxGroupSize, Instant groupLockAt) {
	}

	public record PartDto(Long id, int orderNo, String title, TaskType taskType, String prompt, String imageUrl,
			Integer minWords, Integer suggestedMinutes, Instant partDueAt, BigDecimal weight,
			List<CriterionDto> criteria, boolean examMode, Integer examDurationMinutes, List<SectionDto> sections) {
	}

	public record SectionDto(Long id, String sectionKey, String title, boolean required, Integer minWords,
			Integer maxWords, int orderNo) {
	}

	public record CriterionDto(Long id, CriterionCode code, String name, String description, BigDecimal maxScore,
			BigDecimal step, BigDecimal weight, int orderNo, List<ChecklistItemDto> checklist) {
	}

	public record ChecklistItemDto(Long id, String content, ChecklistKind kind, BigDecimal maxBand, int orderNo) {
	}

	/** Rubric mẫu, cùng dạng tiêu chí để client nạp thẳng vào phần. */
	public record RubricTemplateDto(Long id, String name, TaskType taskType, String description, boolean system,
			List<CriterionDto> criteria) {
	}
}
