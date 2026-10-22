package com.ltmtu.ielts.submission.dto;

import java.time.Instant;

import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.submission.SubmissionStatus;

/** Dữ liệu bài nộp phía giáo viên (hàng đợi chấm) — docs/05-api.md mục A.7. */
public final class TeacherSubmissionDtos {

	private TeacherSubmissionDtos() {
	}

	/** Một dòng hàng đợi chấm: bài nộp + version mới nhất. */
	public record QueueItem(Long submissionId, SubmissionStatus status, int currentVersion, Long studentId,
			String studentCode, String studentName, Long assignmentId, String assignmentTitle, Long partId,
			String partTitle, int partOrderNo, TaskType taskType, Long versionId, Instant submittedAt, boolean late,
			int wordCount) {
	}
}
