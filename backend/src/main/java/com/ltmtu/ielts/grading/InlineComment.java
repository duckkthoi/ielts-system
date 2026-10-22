package com.ltmtu.ielts.grading;

import java.time.Instant;

import com.ltmtu.ielts.assignment.RubricCriterion;

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
 * Nhận xét gắn vào một đoạn của bài (BR-21). Offset tính theo ký tự trên nội dung version đã nộp — version bất
 * biến nên offset luôn đúng. Cần ít nhất một trong: mã lỗi, lời nhận xét, gợi ý sửa.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inline_comments")
public class InlineComment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "grade_id")
	private Grade grade;

	@Column(name = "start_offset", nullable = false)
	private int startOffset;

	@Column(name = "end_offset", nullable = false)
	private int endOffset;

	@Column(columnDefinition = "text")
	private String quote;

	@Column(columnDefinition = "text")
	private String comment;

	/** Bản chụp mã lỗi dạng chữ: giáo viên xoá mã riêng thì nhận xét cũ vẫn giữ nguyên. */
	@Column(name = "error_code", length = 20)
	private String errorCode;

	/** Tiêu chí mà lỗi ảnh hưởng tới (tự điền theo mã lỗi, giáo viên sửa được). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "criterion_id")
	private RubricCriterion criterion;

	/** Gợi ý sửa; ẩn với sinh viên khi bài đang "Cần sửa" nếu bài tập không bật hiển thị (BR-21). */
	@Column(columnDefinition = "text")
	private String suggestion;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
