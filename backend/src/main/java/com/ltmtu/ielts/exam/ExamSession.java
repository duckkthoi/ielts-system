package com.ltmtu.ielts.exam;

import java.time.Instant;

import com.ltmtu.ielts.assignment.AssignmentPart;
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
import lombok.Setter;

/**
 * Phiên thi của một sinh viên cho một phần (UC-27). Tạo bằng {@code INSERT … ON CONFLICT} ở
 * {@link ExamSessionRepository#insertIfAbsent}, nên entity chỉ dùng để đọc và cập nhật.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam_sessions")
public class ExamSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "part_id")
	private AssignmentPart part;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id")
	private User student;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	/** Cố định từ lúc bắt đầu (BR-23). */
	@Column(name = "ends_at", nullable = false)
	private Instant endsAt;

	/** Lần nộp đầu tiên. */
	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "overtime_notified_at")
	private Instant overtimeNotifiedAt;
}
