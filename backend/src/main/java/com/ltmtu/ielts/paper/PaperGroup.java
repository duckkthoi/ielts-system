package com.ltmtu.ielts.paper;

import java.time.Instant;

import com.ltmtu.ielts.assignment.Assignment;
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

/** Một nhóm viết paper trong một bài tập (BR-27). Bảng chỉ có {@code created_at}, giờ lấy từ Clock. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "paper_groups")
public class PaperGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assignment_id")
	private Assignment assignment;

	@Column(nullable = false, length = 100)
	private String name;

	/** Giáo viên hoặc sinh viên tự lập; {@code null} nếu tài khoản đã bị xoá. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** Nhóm do sinh viên tự lập (không phải giáo viên chủ bài) — tự xoá khi không còn ai. */
	public boolean isStudentMade() {
		return createdBy != null && !createdBy.getId().equals(assignment.getTeacher().getId());
	}
}
