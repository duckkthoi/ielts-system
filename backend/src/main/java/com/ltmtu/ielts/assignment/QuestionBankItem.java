package com.ltmtu.ielts.assignment;

import java.util.LinkedHashSet;
import java.util.Set;

import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;
import com.ltmtu.ielts.common.BaseEntity;
import com.ltmtu.ielts.user.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một đề dùng lại trong ngân hàng riêng của giáo viên, kèm rubric và tag (UC-20). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "question_bank_items")
public class QuestionBankItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id")
	private User teacher;

	@OneToOne(fetch = FetchType.LAZY, optional = false, cascade = jakarta.persistence.CascadeType.ALL,
			orphanRemoval = true)
	@JoinColumn(name = "rubric_template_id", nullable = false, unique = true)
	private RubricTemplate rubricTemplate;

	@Column(nullable = false, length = 150)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", nullable = false, length = 30)
	private TaskType taskType;

	@Column(nullable = false, columnDefinition = "text")
	private String prompt;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "min_words")
	private Integer minWords;

	@Column(name = "suggested_minutes")
	private Integer suggestedMinutes;

	@ElementCollection
	@CollectionTable(name = "question_bank_tags", joinColumns = @JoinColumn(name = "item_id"))
	@Column(name = "tag", nullable = false, length = 50)
	private Set<String> tags = new LinkedHashSet<>();
}
