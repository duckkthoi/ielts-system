package com.ltmtu.ielts.classroom;

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

/** Sinh viên thuộc lớp. Khoá chính kép (classroom_id, student_id). */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "class_members")
public class ClassMember {

	@EmbeddedId
	private Id id;

	@MapsId("classroomId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "classroom_id")
	private Classroom classroom;

	@MapsId("studentId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id")
	private User student;

	@Column(name = "joined_at", nullable = false)
	private Instant joinedAt;

	public ClassMember(Classroom classroom, User student, Instant joinedAt) {
		this.id = new Id(classroom.getId(), student.getId());
		this.classroom = classroom;
		this.student = student;
		this.joinedAt = joinedAt;
	}

	@Getter
	@NoArgsConstructor
	@EqualsAndHashCode
	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "classroom_id")
		private Long classroomId;

		@Column(name = "student_id")
		private Long studentId;

		public Id(Long classroomId, Long studentId) {
			this.classroomId = classroomId;
			this.studentId = studentId;
		}
	}
}
