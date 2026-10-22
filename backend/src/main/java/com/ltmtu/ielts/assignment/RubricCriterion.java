package com.ltmtu.ielts.assignment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tiêu chí chấm của một phần (bảng không có cột audit nên không kế thừa BaseEntity). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rubric_criteria")
public class RubricCriterion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "part_id")
	private AssignmentPart part;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private CriterionCode code;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "max_score", nullable = false, precision = 4, scale = 1)
	private BigDecimal maxScore = BigDecimal.valueOf(9);

	/** BR-11: mặc định 1 (nguyên band); giáo viên có thể chọn 0.5. */
	@Column(nullable = false, precision = 3, scale = 1)
	private BigDecimal step = BigDecimal.ONE;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal weight = BigDecimal.ONE;

	@Column(name = "order_no", nullable = false)
	private int orderNo;

	@OneToMany(mappedBy = "criterion", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orderNo")
	private List<ChecklistItem> checklist = new ArrayList<>();

	public void addItem(ChecklistItem item) {
		item.setCriterion(this);
		checklist.add(item);
	}
}
