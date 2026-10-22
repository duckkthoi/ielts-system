package com.ltmtu.ielts.assignment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.common.BaseEntity;

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

/** Một phần của bài tập (vd Task 1). Sinh viên nộp riêng từng phần (BR-09). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assignment_parts")
public class AssignmentPart extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assignment_id")
	private Assignment assignment;

	@Column(name = "order_no", nullable = false)
	private int orderNo;

	@Column(nullable = false, length = 200)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", nullable = false, length = 30)
	private TaskType taskType = TaskType.CUSTOM;

	/** Đề bài dạng HTML đã lọc (chỉ thẻ định dạng cơ bản). */
	@Column(nullable = false, columnDefinition = "text")
	private String prompt;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "min_words")
	private Integer minWords;

	@Column(name = "suggested_minutes")
	private Integer suggestedMinutes;

	/** NULL = dùng hạn của bài tập (BR-07). */
	@Column(name = "part_due_at")
	private Instant partDueAt;

	/** Trọng số khi gộp band các phần (BR-12). */
	@Column(nullable = false, precision = 4, scale = 2)
	private BigDecimal weight = BigDecimal.ONE;

	@OneToMany(mappedBy = "part", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orderNo")
	private List<RubricCriterion> criteria = new ArrayList<>();

	public void addCriterion(RubricCriterion criterion) {
		criterion.setPart(this);
		criteria.add(criterion);
	}
}
