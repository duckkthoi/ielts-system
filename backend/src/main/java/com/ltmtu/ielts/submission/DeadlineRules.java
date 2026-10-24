package com.ltmtu.ielts.submission;

import java.time.Instant;

import com.ltmtu.ielts.assignment.AssignmentPart;

/** Hạn nộp áp dụng (BR-07) và nộp muộn (BR-08). Mọi phép so sánh dùng giờ server (NFR-12). */
public final class DeadlineRules {

	private DeadlineRules() {
	}

	/**
	 * Hạn áp dụng cho một phần của một sinh viên: đang bị yêu cầu sửa và có hạn sửa → hạn sửa; nếu không → hạn riêng
	 * của phần; nếu không → hạn của bài. {@code null} = không có hạn.
	 */
	public static Instant effectiveDue(AssignmentPart part, Submission submission) {
		return submission == null
				? effectiveDue(part.getAssignment().getDueAt(), part.getPartDueAt(), SubmissionStatus.NOT_STARTED, null)
				: effectiveDue(part.getAssignment().getDueAt(), part.getPartDueAt(), submission.getStatus(),
						submission.getRevisionDueAt());
	}

	/** Như trên, cho truy vấn tổng hợp đọc thẳng các cột (không tải entity). */
	public static Instant effectiveDue(Instant assignmentDue, Instant partDue, SubmissionStatus status,
			Instant revisionDue) {
		if (status == SubmissionStatus.REVISION_REQUESTED && revisionDue != null) {
			return revisionDue;
		}
		return partDue != null ? partDue : assignmentDue;
	}

	public static boolean isPastDue(Instant due, Instant now) {
		return due != null && now.isAfter(due);
	}

	/** Còn được soạn / nộp không: chưa quá hạn, hoặc bài cho phép nộp muộn. */
	public static boolean canStillSubmit(AssignmentPart part, Submission submission, Instant now) {
		return !isPastDue(effectiveDue(part, submission), now) || part.getAssignment().isAllowLate();
	}
}
