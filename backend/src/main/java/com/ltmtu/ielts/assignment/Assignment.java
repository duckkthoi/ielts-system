package com.ltmtu.ielts.assignment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.AssignmentEnums.CitationStyle;
import com.ltmtu.ielts.common.BaseEntity;
import com.ltmtu.ielts.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bài tập — gốc của aggregate gồm các phần → tiêu chí → mục checklist.
 * Vòng đời các phần do bài tập quản lý (cascade + orphanRemoval), xem AI-CODING-RULES mục 7.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assignments")
public class Assignment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id")
	private User teacher;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AssignmentStatus status = AssignmentStatus.DRAFT;

	@Column(name = "open_at")
	private Instant openAt;

	@Column(name = "due_at")
	private Instant dueAt;

	@Column(name = "allow_late", nullable = false)
	private boolean allowLate;

	@Column(name = "max_revisions", nullable = false)
	private int maxRevisions = 2;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "open_notified_at")
	private Instant openNotifiedAt;

	/** ESSAY (mặc định) hoặc PAPER (V16, docs/12). */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private AssignmentKind kind = AssignmentKind.ESSAY;

	/** Chỉ PAPER: kiểu trích dẫn (BR-30). */
	@Enumerated(EnumType.STRING)
	@Column(name = "citation_style", length = 10)
	private CitationStyle citationStyle;

	/** Chỉ PAPER: số thành viên tối đa của một nhóm (BR-27). */
	@Column(name = "max_group_size")
	private Integer maxGroupSize;

	/** Chỉ PAPER: sau mốc này sinh viên không tự đổi nhóm; {@code null} = tới hạn nộp (BR-27). */
	@Column(name = "group_lock_at")
	private Instant groupLockAt;

	/** Job đã báo giáo viên lúc tới hạn chốt nhóm chưa (chống báo trùng, V17). */
	@Column(name = "group_lock_notified_at")
	private Instant groupLockNotifiedAt;

	/** BR-21: có hiện gợi ý sửa cho sinh viên khi bài đang "Cần sửa" không. */
	@Column(name = "show_suggestions_on_revision", nullable = false)
	private boolean showSuggestionsOnRevision;

	@OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orderNo")
	private List<AssignmentPart> parts = new ArrayList<>();

	public void addPart(AssignmentPart part) {
		part.setAssignment(this);
		parts.add(part);
	}

	public boolean isDraft() {
		return status == AssignmentStatus.DRAFT;
	}

	public boolean isPaper() {
		return kind == AssignmentKind.PAPER;
	}

	/** Mốc chốt nhóm thực tế: {@code groupLockAt}, không đặt thì là hạn nộp (BR-27). */
	public Instant effectiveGroupLockAt() {
		return groupLockAt != null ? groupLockAt : dueAt;
	}
}
