package com.ltmtu.ielts.paper;

import java.io.Serializable;
import java.time.Instant;

import com.ltmtu.ielts.user.User;

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

/**
 * Sinh viên thuộc nhóm. Khoá chính kép (group_id, student_id); {@code assignment_id} lặp lại để DB chặn một sinh
 * viên ở hai nhóm của cùng một bài (UNIQUE assignment_id + student_id — BR-27).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "paper_group_members")
public class PaperGroupMember {

	@EmbeddedId
	private Id id;

	@MapsId("groupId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id")
	private PaperGroup group;

	@MapsId("studentId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id")
	private User student;

	@Column(name = "assignment_id", nullable = false)
	private Long assignmentId;

	@Column(name = "joined_at", nullable = false)
	private Instant joinedAt;

	public PaperGroupMember(PaperGroup group, User student, Instant joinedAt) {
		this.id = new Id(group.getId(), student.getId());
		this.group = group;
		this.student = student;
		this.assignmentId = group.getAssignment().getId();
		this.joinedAt = joinedAt;
	}

	@Getter
	@NoArgsConstructor
	@EqualsAndHashCode
	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "group_id")
		private Long groupId;

		@Column(name = "student_id")
		private Long studentId;

		public Id(Long groupId, Long studentId) {
			this.groupId = groupId;
			this.studentId = studentId;
		}
	}
}
