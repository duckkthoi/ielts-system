package com.ltmtu.ielts.assignment;

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

/** Một mục trong khung của một giai đoạn bài PAPER (vd Abstract, Method) — sống chết theo phần bài (BR-29). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "paper_sections")
public class PaperSection {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "part_id")
	private AssignmentPart part;

	/** Khoá ổn định của mục trong tài liệu (node `paperSection` của trình soạn) — không đổi khi đổi tiêu đề. */
	@Column(name = "section_key", nullable = false, length = 40)
	private String sectionKey;

	@Column(nullable = false, length = 200)
	private String title;

	/** Mục bắt buộc phải có nội dung mới nộp được. */
	@Column(nullable = false)
	private boolean required = true;

	@Column(name = "min_words")
	private Integer minWords;

	@Column(name = "max_words")
	private Integer maxWords;

	@Column(name = "order_no", nullable = false)
	private int orderNo;
}
