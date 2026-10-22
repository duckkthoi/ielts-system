package com.ltmtu.ielts.submission;

import java.time.Instant;

import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.common.BaseEntity;
import com.ltmtu.ielts.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bài làm của một sinh viên cho một phần (UNIQUE part + student). Được tạo ở lần lưu nháp hoặc nộp đầu tiên;
 * chưa có dòng nghĩa là {@code NOT_STARTED}. Mỗi lần nộp tạo một {@link SubmissionVersion} bất biến.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "submissions")
public class Submission extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "part_id")
	private AssignmentPart part;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id")
	private User student;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SubmissionStatus status = SubmissionStatus.NOT_STARTED;

	/** Số version mới nhất đã nộp (0 = chưa nộp). Client gửi {@code expectedVersion} để chống nộp trùng (SD-07). */
	@Column(name = "current_version", nullable = false)
	private int currentVersion;

	@Column(name = "draft_content", columnDefinition = "text")
	private String draftContent;

	@Column(name = "draft_saved_at")
	private Instant draftSavedAt;

	/** Hạn sửa khi bị yêu cầu sửa (BR-07: ưu tiên hơn hạn phần / hạn bài). */
	@Column(name = "revision_due_at")
	private Instant revisionDueAt;

	@Column(name = "revision_note", columnDefinition = "text")
	private String revisionNote;

	public static Submission start(AssignmentPart part, User student) {
		Submission s = new Submission();
		s.part = part;
		s.student = student;
		return s;
	}
}
