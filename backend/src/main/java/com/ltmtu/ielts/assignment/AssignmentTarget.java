package com.ltmtu.ielts.assignment;

import java.time.Instant;

import com.ltmtu.ielts.classroom.Classroom;
import com.ltmtu.ielts.user.User;

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

/**
 * Giao bài: mỗi dòng trỏ tới đúng MỘT lớp hoặc MỘT sinh viên (CHECK num_nonnulls = 1).
 * Giao cho lớp nghĩa là mọi thành viên lớp — kể cả người được thêm vào lớp sau khi phát hành — đều thấy bài (BR-04).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "assignment_targets")
public class AssignmentTarget {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assignment_id")
	private Assignment assignment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "classroom_id")
	private Classroom classroom;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "student_id")
	private User student;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public static AssignmentTarget forClass(Assignment a, Classroom c, Instant now) {
		AssignmentTarget t = new AssignmentTarget();
		t.assignment = a;
		t.classroom = c;
		t.createdAt = now;
		return t;
	}

	public static AssignmentTarget forStudent(Assignment a, User s, Instant now) {
		AssignmentTarget t = new AssignmentTarget();
		t.assignment = a;
		t.student = s;
		t.createdAt = now;
		return t;
	}
}
