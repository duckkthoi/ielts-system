package com.ltmtu.ielts.submission;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Một lần nộp — lịch sử nộp bài, <b>bất biến</b> (NFR-11): chỉ INSERT, Hibernate bỏ qua mọi thay đổi sau khi lưu.
 * Nhận xét inline của giáo viên dùng offset ký tự trên {@link #content}, nên nội dung không bao giờ được đổi.
 */
@Getter
@NoArgsConstructor
@Immutable
@Entity
@Table(name = "submission_versions")
public class SubmissionVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "submission_id")
	private Submission submission;

	@Column(name = "version_no", nullable = false)
	private int versionNo;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	/** Do server đếm (BR-20), không tin số client gửi. */
	@Column(name = "word_count", nullable = false)
	private int wordCount;

	/** Giờ server (NFR-12). */
	@Column(name = "submitted_at", nullable = false)
	private Instant submittedAt;

	@Column(name = "is_late", nullable = false)
	private boolean late;

	public static SubmissionVersion of(Submission submission, int versionNo, String content, Instant submittedAt,
			boolean late) {
		SubmissionVersion v = new SubmissionVersion();
		v.submission = submission;
		v.versionNo = versionNo;
		v.content = content;
		v.wordCount = WordCounter.count(content);
		v.submittedAt = submittedAt;
		v.late = late;
		return v;
	}
}
