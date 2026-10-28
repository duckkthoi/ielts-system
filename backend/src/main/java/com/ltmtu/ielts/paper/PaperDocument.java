package com.ltmtu.ielts.paper;

import org.hibernate.annotations.ColumnTransformer;

import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tài liệu paper của một nhóm cho một giai đoạn (docs/12 mục 5.2). Nội dung Yjs: {@code snapshot} (đã gộp tới
 * {@code snapshotSeq}) + các {@link PaperDocUpdate} sau đó. Tạo bằng {@code INSERT … ON CONFLICT} ở
 * {@link PaperDocumentRepository#insertIfAbsent}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "paper_documents")
public class PaperDocument extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "part_id")
	private AssignmentPart part;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id")
	private PaperGroup group;

	/** Trạng thái Yjs đã gộp; {@code null} = chưa có snapshot. Server không đọc nội dung. */
	@Column(columnDefinition = "bytea")
	private byte[] snapshot;

	@Column(name = "snapshot_seq", nullable = false)
	private long snapshotSeq;

	/** Số thứ tự update lớn nhất đã cấp — chỉ tăng khi đang khoá dòng. */
	@Column(name = "last_seq", nullable = false)
	private long lastSeq;

	/** JSON của trình soạn lúc snapshot (client gửi kèm) — dùng khi nộp / xuất file. */
	@Column(name = "content_json", columnDefinition = "jsonb")
	@ColumnTransformer(write = "?::jsonb")
	private String contentJson;

	/** Chữ thuần lúc snapshot — đếm từ, xem trực tiếp, đạo văn. */
	@Column(name = "content_text", columnDefinition = "text")
	private String contentText;
}
