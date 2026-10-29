package com.ltmtu.ielts.paper;

import org.hibernate.annotations.ColumnTransformer;

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

/** Một tài liệu tham khảo trong thư viện trích dẫn của nhóm (docs/12 mục 5.3, BR-30). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "paper_references")
public class PaperReference extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id")
	private PaperGroup group;

	/** Bản ghi CSL-JSON đã lọc qua {@link CitationRules#sanitize}. */
	@Column(nullable = false, columnDefinition = "jsonb")
	@ColumnTransformer(write = "?::jsonb")
	private String csl;

	/** DOI đã chuẩn hoá — chặn thêm trùng trong một nhóm. */
	@Column(length = 200)
	private String doi;

	/** {@code null} nếu tài khoản người thêm đã bị xoá. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;
}
