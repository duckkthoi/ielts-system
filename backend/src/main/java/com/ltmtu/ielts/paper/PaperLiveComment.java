package com.ltmtu.ielts.paper;

import com.ltmtu.ielts.common.BaseEntity;
import com.ltmtu.ielts.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
