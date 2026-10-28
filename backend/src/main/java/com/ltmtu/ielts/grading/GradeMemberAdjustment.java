package com.ltmtu.ielts.grading;

import java.io.Serializable;
import java.math.BigDecimal;

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
import lombok.Setter;

/**
 * Điểm cộng / trừ riêng của một thành viên nhóm trên kết quả chấm bài PAPER (B25b, docs/12 mục 5.4): tối đa ±2, bắt buộc
 * lý do. Điểm của thành viên = điểm cả bài + {@code delta}, kẹp trong [0, 10]. Khoá chính kép (grade_id, student_id).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "grade_member_adjustments")
public class GradeMemberAdjustment {

	@EmbeddedId
	private Id id;

	@MapsId("gradeId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "grade_id")
	private Grade grade;

	@MapsId("studentId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id")
	private User student;

	@Column(nullable = false, precision = 3, scale = 2)
	private BigDecimal delta;

	@Column(nullable = false, columnDefinition = "text")
	private String reason;

	public GradeMemberAdjustment(Grade grade, User student, BigDecimal delta, String reason) {
		this.id = new Id(grade.getId(), student.getId());
		this.grade = grade;
		this.student = student;
		this.delta = delta;
		this.reason = reason;
	}

	@Getter
	@NoArgsConstructor
	@EqualsAndHashCode
	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "grade_id")
		private Long gradeId;

		@Column(name = "student_id")
		private Long studentId;

		public Id(Long gradeId, Long studentId) {
			this.gradeId = gradeId;
			this.studentId = studentId;
		}
	}
}
