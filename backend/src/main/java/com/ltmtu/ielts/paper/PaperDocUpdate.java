package com.ltmtu.ielts.paper;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.data.domain.Persistable;

import com.ltmtu.ielts.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Một gói cập nhật Yjs của tài liệu, số thứ tự {@code seq} do server cấp. Đã gộp vào snapshot thì {@code payload =
 * null}, dòng vẫn giữ để thống kê đóng góp (BR-32). Chỉ INSERT, không sửa nội dung.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "paper_doc_updates")
public class PaperDocUpdate implements Persistable<PaperDocUpdate.Id> {

	@EmbeddedId
	private Id id;

	@Column(columnDefinition = "bytea")
	private byte[] payload;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id")
	private User author;

	@Column(name = "chars_added", nullable = false)
	private int charsAdded;

	@Column(name = "chars_removed", nullable = false)
	private int charsRemoved;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/**
	 * Luôn là bản ghi mới: khoá chính do server cấp, nên {@code save()} phải INSERT — trùng số thứ tự thì DB báo lỗi,
	 * không bao giờ âm thầm ghi đè update khác (merge).
	 */
	@Override
	public boolean isNew() {
		return true;
	}

	public PaperDocUpdate(Long documentId, long seq, byte[] payload, User author, int charsAdded, int charsRemoved,
			Instant createdAt) {
		this.id = new Id(documentId, seq);
		this.payload = payload;
		this.author = author;
		this.charsAdded = charsAdded;
		this.charsRemoved = charsRemoved;
		this.createdAt = createdAt;
	}

	@Getter
	@NoArgsConstructor
	@EqualsAndHashCode
	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "document_id")
		private Long documentId;

		@Column(name = "seq")
		private long seq;

		public Id(Long documentId, long seq) {
			this.documentId = documentId;
			this.seq = seq;
		}
	}
}
