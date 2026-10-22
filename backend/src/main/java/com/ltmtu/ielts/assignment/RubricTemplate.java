package com.ltmtu.ielts.assignment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.ChecklistKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.CriterionCode;
import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.common.BaseEntity;
import com.ltmtu.ielts.user.User;

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

/**
 * Rubric mẫu: owner = NULL là mẫu của hệ thống (IELTS Task 1 / Task 2, seed V2 + V4).
 * "Nạp mẫu" = sao chép tiêu chí + checklist sang phần của bài tập; sửa mẫu sau đó không ảnh hưởng bài đã tạo.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rubric_templates")
public class RubricTemplate extends BaseEntity {

	@Column(nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", nullable = false, length = 30)
	private TaskType taskType;

	@Column(columnDefinition = "text")
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	private User owner;

	@OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orderNo")
	private List<Criterion> criteria = new ArrayList<>();

	@Getter
	@Setter
	@NoArgsConstructor
	@Entity
	@Table(name = "rubric_template_criteria")
	public static class Criterion {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "template_id")
		private RubricTemplate template;

		@Enumerated(EnumType.STRING)
		@Column(length = 10)
		private CriterionCode code;

		@Column(nullable = false, length = 150)
		private String name;

		@Column(columnDefinition = "text")
		private String description;

		@Column(name = "max_score", nullable = false, precision = 4, scale = 1)
		private BigDecimal maxScore;

		@Column(nullable = false, precision = 3, scale = 1)
		private BigDecimal step;

		@Column(nullable = false, precision = 5, scale = 2)
		private BigDecimal weight;

		@Column(name = "order_no", nullable = false)
		private int orderNo;

		@OneToMany(mappedBy = "criterion", cascade = CascadeType.ALL, orphanRemoval = true)
		@OrderBy("orderNo")
		private List<Item> checklist = new ArrayList<>();
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@Entity
	@Table(name = "rubric_template_checklist_items")
	public static class Item {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "criterion_id")
		private Criterion criterion;

		@Column(nullable = false, columnDefinition = "text")
		private String content;

		@Column(name = "order_no", nullable = false)
		private int orderNo;

		@Enumerated(EnumType.STRING)
		@Column(nullable = false, length = 10)
		private ChecklistKind kind;

		@Column(name = "max_band", precision = 3, scale = 1)
		private BigDecimal maxBand;
	}
}
