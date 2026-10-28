package com.ltmtu.ielts.grading;

import java.io.Serializable;
import java.math.BigDecimal;

import com.ltmtu.ielts.assignment.RubricCriterion;
import com.ltmtu.ielts.assignment.RubricLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Điểm và nhận xét của một tiêu chí. Khoá chính kép (grade_id, criterion_id). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "grade_criterion_scores")
public class GradeCriterionScore {

	@EmbeddedId
	private Id id;

	@MapsId("gradeId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "grade_id")
	private Grade grade;

	@MapsId("criterionId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criterion_id")
	private RubricCriterion criterion;

	/** NULL = chưa chấm tiêu chí này (lưu nháp dở). */
	@Column(precision = 4, scale = 2)
	private BigDecimal score;

	/** Bài PAPER: mức đạt giáo viên chọn (BR-31); NULL = chấm điểm trực tiếp. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "level_id")
	private RubricLevel level;

	@Column(columnDefinition = "text")
	private String comment;

	public GradeCriterionScore(Grade grade, RubricCriterion criterion) {
		this.id = new Id(grade.getId(), criterion.getId());
		this.grade = grade;
		this.criterion = criterion;
	}

	@Getter
	@NoArgsConstructor
	@EqualsAndHashCode
	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "grade_id")
		private Long gradeId;

		@Column(name = "criterion_id")
		private Long criterionId;

		public Id(Long gradeId, Long criterionId) {
			this.gradeId = gradeId;
			this.criterionId = criterionId;
		}
	}
}
