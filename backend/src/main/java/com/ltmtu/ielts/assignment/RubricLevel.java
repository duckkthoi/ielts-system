package com.ltmtu.ielts.assignment;

import java.math.BigDecimal;

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
import lombok.Setter;

/**
 * Một mức đạt của tiêu chí trong rubric phân tích (BR-31, docs/12 mục 5.4): nhãn, khoảng điểm, mô tả "bài ở mức này
 * trông như thế nào". Chỉ tiêu chí của bài PAPER có; sống chết theo tiêu chí.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rubric_levels")
public class RubricLevel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criterion_id")
	private RubricCriterion criterion;

	@Column(nullable = false, length = 50)
	private String label;

	@Column(name = "min_score", nullable = false, precision = 4, scale = 2)
	private BigDecimal minScore;

	@Column(name = "max_score", nullable = false, precision = 4, scale = 2)
	private BigDecimal maxScore;

	@Column(columnDefinition = "text")
	private String descriptor;

	@Column(name = "order_no", nullable = false)
	private int orderNo;

	/** Điểm nằm trong khoảng của mức (tính cả hai đầu). */
	public boolean contains(BigDecimal score) {
		return score.compareTo(minScore) >= 0 && score.compareTo(maxScore) <= 0;
	}
}
