package com.ltmtu.ielts.grading;

import com.ltmtu.ielts.common.BaseEntity;
import com.ltmtu.ielts.grading.GradingEnums.CriterionGroup;
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

/** Mã lỗi gắn vào nhận xét inline (BR-21). Không có chủ = mã của hệ thống, không sửa / xoá được (BR-22). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "error_codes")
public class ErrorCode extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	private User owner;

	@Column(nullable = false, length = 20)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	/** NULL = không gắn tiêu chí (vd {@code ?}, {@code GOOD}). */
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private CriterionGroup criterion;

	@Column(columnDefinition = "text")
	private String description;

	/** Mã khen ({@code GOOD}): không tính là lỗi khi tổng hợp. */
	@Column(nullable = false)
	private boolean positive;

	@Column(name = "order_no", nullable = false)
	private int orderNo;

	public boolean isSystem() {
		return owner == null;
	}
}
