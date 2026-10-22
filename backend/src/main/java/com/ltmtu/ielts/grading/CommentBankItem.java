package com.ltmtu.ielts.grading;

import java.time.Instant;

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

/** Một nhận xét trong ngân hàng nhận xét (BR-22). Không có chủ = nhận xét mẫu của hệ thống, chỉ đọc. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comment_bank")
public class CommentBankItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	private User owner;

	/** NULL = dùng chung cho mọi tiêu chí. */
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private CriterionGroup criterion;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	/** Số lần chèn, để xếp nhận xét hay dùng lên đầu. */
	@Column(name = "use_count", nullable = false)
	private int useCount;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	public boolean isSystem() {
		return owner == null;
	}
}
