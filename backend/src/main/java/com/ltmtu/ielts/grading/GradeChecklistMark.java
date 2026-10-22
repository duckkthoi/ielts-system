package com.ltmtu.ielts.grading;

import java.io.Serializable;

import com.ltmtu.ielts.assignment.ChecklistItem;

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

/** Tick một mục checklist. Mục LIMIT được tick thì giới hạn điểm tiêu chí (BR-17). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "grade_checklist_marks")
public class GradeChecklistMark {

	@EmbeddedId
	private Id id;

	@MapsId("gradeId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "grade_id")
	private Grade grade;

	@MapsId("checklistItemId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checklist_item_id")
	private ChecklistItem item;

	@Column(nullable = false)
	private boolean checked;

	public GradeChecklistMark(Grade grade, ChecklistItem item) {
		this.id = new Id(grade.getId(), item.getId());
		this.grade = grade;
		this.item = item;
	}

	@Getter
	@NoArgsConstructor
	@EqualsAndHashCode
	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "grade_id")
		private Long gradeId;

		@Column(name = "checklist_item_id")
		private Long checklistItemId;

		public Id(Long gradeId, Long checklistItemId) {
			this.gradeId = gradeId;
			this.checklistItemId = checklistItemId;
		}
	}
}
