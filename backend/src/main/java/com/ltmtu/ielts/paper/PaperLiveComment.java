package com.ltmtu.ielts.paper;

import java.time.Instant;

import com.ltmtu.ielts.common.BaseEntity;
import com.ltmtu.ielts.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Nhận xét tạm thời của giáo viên trên tài liệu Paper đang viết. Offset chỉ là mốc tham chiếu, quote là bất biến. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "paper_live_comments")
public class PaperLiveComment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id")
	private PaperDocument document;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id")
	private User author;

	@Column(name = "start_offset", nullable = false)
	private int startOffset;

	@Column(name = "end_offset", nullable = false)
	private int endOffset;

	@Column(nullable = false, columnDefinition = "text")
	private String quote;

	@Column(nullable = false, columnDefinition = "text")
	private String comment;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Kind kind = Kind.COMMENT;

	/** Cách sửa giáo viên gợi ý (nút "Gợi ý sửa"); null nếu không có. */
	@Column(columnDefinition = "text")
	private String suggestion;

	/** Mục của paper chứa đoạn được chọn, để lọc / hiển thị; null nếu không xác định. */
	@Column(name = "section_key", length = 100)
	private String sectionKey;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "resolved_by")
	private User resolvedBy;

	/** Loại phản hồi hướng dẫn (không phải mã lỗi chấm chính thức). */
	public enum Kind {
		COMMENT, GRAMMAR, VOCABULARY, COHERENCE, CITATION, FORMAT
	}

	public PaperLiveComment(PaperDocument document, User author, int startOffset, int endOffset, String quote,
			String comment) {
		this.document = document;
		this.author = author;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.quote = quote;
		this.comment = comment;
	}
}
