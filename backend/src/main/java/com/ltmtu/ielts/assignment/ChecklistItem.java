package com.ltmtu.ielts.assignment;

import java.math.BigDecimal;

import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mục checklist của một tiêu chí (BR-16). Loại LIMIT có {@code maxBand}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "checklist_items")
public class ChecklistItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criterion_id")
	private RubricCriterion criterion;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "order_no", nullable = false)
	private int orderNo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ChecklistKind kind = ChecklistKind.CHECK;

	@Column(name = "max_band", precision = 4, scale = 2)
	private BigDecimal maxBand;
}
