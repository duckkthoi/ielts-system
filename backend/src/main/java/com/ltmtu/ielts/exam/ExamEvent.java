package com.ltmtu.ielts.exam;

import java.time.Instant;

import com.ltmtu.ielts.exam.ExamEnums.ExamEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Một sự kiện trong phiên thi: bắt đầu, rời / quay lại tab, dán, mất / có lại kết nối, hết giờ, nộp (UC-21). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam_events")
public class ExamEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "exam_session_id")
	private ExamSession session;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ExamEventType type;

	/** Số ký tự đã dán (chỉ PASTE). Không lưu nội dung dán. */
	@Column(name = "paste_chars")
	private Integer pasteChars;

	/** Giờ server lúc nhận; sự kiện gửi bù thì là giờ client báo, đã kẹp trong phiên thi ({@link ExamRules#reportedAt}). */
	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	/** Client gửi bù sau khi có mạng lại. */
	@Column(name = "reported_late", nullable = false)
	private boolean reportedLate;

	static ExamEvent of(ExamSession session, ExamEventType type, Integer pasteChars, Instant at, boolean reportedLate) {
		ExamEvent e = new ExamEvent();
		e.setSession(session);
		e.setType(type);
		e.setPasteChars(pasteChars);
		e.setOccurredAt(at);
		e.setReportedLate(reportedLate);
		return e;
	}
}
