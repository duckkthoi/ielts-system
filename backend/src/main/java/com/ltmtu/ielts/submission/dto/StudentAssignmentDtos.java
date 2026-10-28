package com.ltmtu.ielts.submission.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.AssignmentEnums.CitationStyle;
import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;
import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.SectionDto;
import com.ltmtu.ielts.submission.SubmissionStatus;

/** Danh sách / chi tiết bài được giao phía sinh viên — docs/05-api.md mục A.8 (UC-22). */
public final class StudentAssignmentDtos {

	private StudentAssignmentDtos() {
	}

	/** Các tab trên màn "Bài được giao". Một bài có thể thuộc nhiều tab. */
	public enum Tab {
		/** Còn phần chưa nộp, chưa quá hạn, bài đang mở. */
		TODO,
		/** Thuộc tab "Cần làm" và có phần hết hạn trong vòng 24 giờ. */
		DUE_SOON,
		/** Còn phần chưa nộp mà đã quá hạn (dù được nộp muộn), hoặc bài đã đóng. */
		OVERDUE,
		/** Không còn phần nào phải làm. */
		SUBMITTED,
		/** Có phần đang bị yêu cầu sửa. */
		REVISION
	}

	public record PartStatus(Long partId, int orderNo, String title, TaskType taskType, SubmissionStatus status,
			int currentVersion, boolean late, Instant effectiveDueAt, boolean canSubmit) {
	}

	public record AssignmentItem(Long id, String title, String teacherName, AssignmentStatus status, Instant openAt,
			Instant dueAt, Instant nextDueAt, Set<Tab> tabs, List<PartStatus> parts) {
	}

	/** {@code counts}: số bài trong mỗi tab (để hiện badge), luôn tính trên toàn bộ bài, không phụ thuộc tab đang lọc. */
	public record AssignmentList(Map<Tab, Integer> counts, List<AssignmentItem> items, Instant serverTime) {
	}

	public record ChecklistItemView(String content, ChecklistKind kind, BigDecimal maxBand) {
	}

	public record CriterionView(CriterionCode code, String name, String description, BigDecimal maxScore,
			BigDecimal step, List<ChecklistItemView> checklist) {
	}

	public record PartDetail(Long partId, int orderNo, String title, TaskType taskType, String prompt, String imageUrl,
			Integer minWords, Integer suggestedMinutes, Instant effectiveDueAt, SubmissionStatus status,
			int currentVersion, boolean late, boolean canSubmit, String revisionNote, Instant revisionDueAt,
			List<CriterionView> criteria, boolean examMode, Integer examDurationMinutes, List<SectionDto> sections) {
	}

	public record AssignmentDetail(Long id, String title, String description, String teacherName,
			AssignmentStatus status, Instant openAt, Instant dueAt, boolean allowLate, int maxRevisions,
			Set<Tab> tabs, List<PartDetail> parts, Instant serverTime, AssignmentKind kind, CitationStyle citationStyle,
			Integer maxGroupSize, Instant groupLockAt) {
	}
}
