package com.ltmtu.ielts.grading;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ltmtu.ielts.common.BaseEntity;
import com.ltmtu.ielts.grading.GradingEnums.GradeStatus;
import com.ltmtu.ielts.grading.GradingEnums.ZeroReason;
import com.ltmtu.ielts.submission.SubmissionVersion;
import com.ltmtu.ielts.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kết quả chấm một version (tối đa một kết quả cho mỗi version). Gốc của aggregate gồm điểm tiêu chí, tick
 * checklist và nhận xét inline (cascade + orphanRemoval, AI-CODING-RULES mục 7).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "grades")
public class Grade extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "submission_version_id", unique = true)
	private SubmissionVersion version;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "grader_id")
	private User grader;

	/** Band của phần, chưa làm tròn (BR-12); NULL khi chưa chấm đủ mọi tiêu chí. */
	@Column(name = "overall_score", precision = 8, scale = 6)
	private BigDecimal overallScore;

	@Column(name = "general_comment", columnDefinition = "text")
	private String generalComment;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private GradeStatus status = GradeStatus.DRAFT;

	@Column(name = "returned_at")
	private Instant returnedAt;

	/** BR-19: khác NULL khi "Chấm 0"; khi đó mọi tiêu chí = 0. */
	@Enumerated(EnumType.STRING)
	@Column(name = "zero_reason", length = 20)
	private ZeroReason zeroReason;

	@OneToMany(mappedBy = "grade", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<GradeCriterionScore> scores = new ArrayList<>();

	@OneToMany(mappedBy = "grade", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<GradeChecklistMark> marks = new ArrayList<>();

	@OneToMany(mappedBy = "grade", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("startOffset")
	private List<InlineComment> inlineComments = new ArrayList<>();

	public static Grade start(SubmissionVersion version, User grader) {
		Grade g = new Grade();
		g.version = version;
		g.grader = grader;
		return g;
	}

	public boolean isReturned() {
		return status == GradeStatus.RETURNED;
	}
}
