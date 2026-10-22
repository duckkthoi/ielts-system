package com.ltmtu.ielts.notification;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Đã nhắc (phần, sinh viên, mốc) — chống nhắc trùng (BR-14). Chỉ ghi bằng
 * {@link ReminderLogRepository#insertIfAbsent} ({@code ON CONFLICT DO NOTHING}), không "kiểm tra rồi mới chèn".
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "reminder_log")
public class ReminderLog {

	@EmbeddedId
	private Id id;

	@Column(name = "sent_at", nullable = false)
	private Instant sentAt;

	/** Mốc nhắc — khớp CHECK của cột {@code kind}. */
	public enum Kind {
		H24, H2, REVISION_H24, REVISION_H2;

		public boolean revision() {
			return this == REVISION_H24 || this == REVISION_H2;
		}
	}

	@Getter
	@NoArgsConstructor
	@EqualsAndHashCode
	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "part_id")
		private Long partId;

		@Column(name = "student_id")
		private Long studentId;

		@Column(name = "kind", length = 20)
		private String kind;
	}
}
